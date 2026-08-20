package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.exception.InsufficientInventoryException;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderItemOperation;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationPreorderItem;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.entity.OrderVoucherUsage;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderItemOperationRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;
import poly.edu.quanlynhahang.repository.OrderVoucherUsageRepository;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OrderCheckoutService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderItemOperationRepository orderItemOperationRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final RestaurantTableRepository tableRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final VoucherRepository voucherRepository;
    private final OrderVoucherUsageRepository orderVoucherUsageRepository;
    private final ActivityLogService activityLogService;
    private final OrderPaymentService orderPaymentService;
    private final MenuAvailabilityService menuAvailabilityService;

    public OrderCheckoutService(OrderRepository orderRepository,
                                OrderDetailRepository orderDetailRepository,
                                OrderItemOperationRepository orderItemOperationRepository,
                                ProductRepository productRepository,
                                AccountRepository accountRepository,
                                RestaurantTableRepository tableRepository,
                                RecipeRepository recipeRepository,
                                IngredientRepository ingredientRepository,
                                IngredientBatchRepository ingredientBatchRepository,
                                VoucherRepository voucherRepository,
                                OrderVoucherUsageRepository orderVoucherUsageRepository,
                                ActivityLogService activityLogService,
                                OrderPaymentService orderPaymentService,
                                MenuAvailabilityService menuAvailabilityService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.orderItemOperationRepository = orderItemOperationRepository;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
        this.tableRepository = tableRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.voucherRepository = voucherRepository;
        this.orderVoucherUsageRepository = orderVoucherUsageRepository;
        this.activityLogService = activityLogService;
        this.orderPaymentService = orderPaymentService;
        this.menuAvailabilityService = menuAvailabilityService;
    }

    private String generateSecureOrderCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return code.toString();
    }

    @Transactional
    public CheckoutResult checkout(OrderRequest request, String username) {
        validateRequest(request);
        Account account = authenticatedAccount(username);
        // The caller must choose the business flow explicitly. A silent fallback can
        // misclassify dine-in orders and corrupt historical reporting.
        OrderType orderType = request.getOrderType();
        if (orderType == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng chọn loại đơn hàng (DINE_IN, TAKEAWAY hoặc DELIVERY).");
        }
        boolean dineIn = OrderType.DINE_IN.equals(orderType);
        if (dineIn && request.getTableId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Đơn tại quán (DINE_IN) bắt buộc phải có tableId.");
        }
        RestaurantTable dineInTable = dineIn
                ? tableRepository.findLockedById(request.getTableId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn."))
                : null;
        OrderPaymentOption paymentOption = resolvePaymentOption(request.getPaymentOption(), dineIn);

        List<RequestedItem> requestedItems = normalizeItems(request.getItems());
        List<CheckoutLine> lines = loadProducts(requestedItems);
        // Validate voucher trước (chưa ghi usage, chưa set isUsed)
        Voucher validatedVoucher = validateVoucher(request.getVoucherCode(), account, dineIn);
        double discountRate = calculateBaseDiscount(account, dineIn)
                + (validatedVoucher != null ? validatedVoucher.getDiscountPercent() / 100.0 : 0.0);
        discountRate = Math.min(discountRate, 1.0);
        Map<Long, IngredientRequirement> requirements = inventoryRequirements(lines);
        Map<Long, List<IngredientBatch>> lockedBatches = lockAndValidateInventory(requirements);

        String orderCode = generateSecureOrderCode();
        Order order = new Order();
        order.setAccount(account);
        order.setAddress(dineIn ? null : safeAddress(request.getAddress()));
        order.setCreateDate(new Date());
        order.setStatus(0);
        order.setIsPaid(false);
        order.setDeposit(BigDecimal.ZERO);
        order.setPaymentOption(paymentOption);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setOrderType(orderType);
        if (dineIn && request.getTableId() != null) {
            order.setTableId(request.getTableId());
        }
        Order savedOrder = orderRepository.save(order);

        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(BigDecimal.valueOf(discountRate));
        for (CheckoutLine line : lines) {
            Product product = line.product();
            BigDecimal lineSubtotal = money(product.getPrice()).multiply(BigDecimal.valueOf(line.quantity()))
                    .multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxRate = decimal(product.getTaxRate(), 8.0);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);

            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProduct(product);
            detail.setQuantity(line.quantity());
            detail.setPrice(lineSubtotal);
            detail.setTaxRate(taxRate);
            detail.setTaxAmount(lineTax);
            detail.setStatus(0);
            detail.setNote(line.note());
            detail.setAllergyNote(line.allergyNote());
            detail.setPriority(line.priority());
            detail.setQueuedAt(new Date());
            orderDetailRepository.save(detail);
            subTotal = subTotal.add(lineSubtotal);
            taxAmount = taxAmount.add(lineTax);
        }

        BigDecimal totalAmount = subTotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        savedOrder.setSubTotal(subTotal);
        savedOrder.setTaxAmount(taxAmount);
        savedOrder.setTotalAmount(totalAmount);
        savedOrder.setRemainingAmount(totalAmount.setScale(0, RoundingMode.HALF_UP));
        consumeInventory(requirements, lockedBatches);
        if (dineInTable != null) {
            markTablePending(savedOrder, orderCode, dineInTable);
        }
        orderRepository.save(savedOrder);

        // Ghi VoucherUsage sau khi đã có orderId
        if (validatedVoucher != null) {
            recordVoucherUsage(validatedVoucher, savedOrder, account, discountRate);
        }

        PaymentQrResponse payment = orderPaymentService.createForOrder(savedOrder);
        activityLogService.log("CREATE", "Order", String.valueOf(savedOrder.getId()),
                "Tạo đơn chờ xác nhận #" + orderCode);

        return new CheckoutResult(savedOrder.getId(), orderCode, savedOrder.getStatus(),
                subTotal, taxAmount, totalAmount,
                savedOrder.getPaymentOption(), savedOrder.getPaymentStatus(), payment);
    }

    /**
     * Materializes an approved reservation preorder as one kitchen order. The reservation
     * stores the returned id, making the operation idempotent at the workflow level.
     */
    @Transactional
    public Integer dispatchReservationPreorder(Reservation reservation,
                                               List<ReservationPreorderItem> preorderItems) {
        if (reservation == null || reservation.getTable() == null || preorderItems == null || preorderItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Đặt bàn không có món đặt trước để chuyển xuống bếp");
        }

        List<RequestedItem> requestedItems = preorderItems.stream()
                .map(item -> new RequestedItem(item.getProductId(), item.getQuantity(),
                        normalizedText(item.getNote()), "", 0))
                .toList();
        List<CheckoutLine> lines = loadProducts(requestedItems);
        Map<Long, IngredientRequirement> requirements = inventoryRequirements(lines);
        Map<Long, List<IngredientBatch>> lockedBatches = lockAndValidateInventory(requirements);

        Order order = new Order();
        order.setAccount(authenticatedAccount(reservation.getCreatedBy()));
        order.setAddress("ĐẶT BÀN: " + reservation.getReservationCode() + " | Bàn: "
                + reservation.getTable().getName() + " | [TẠI QUÁN]");
        order.setTableId(reservation.getTable().getId());
        order.setCreateDate(new Date());
        order.setStatus(1);
        order.setDeposit(money(reservation.getPaidAmount()));
        order.setPaymentOption(OrderPaymentOption.PAY_AT_RESTAURANT);
        order.setPaymentStatus(reservation.getPaymentStatus() == null
                ? PaymentStatus.UNPAID : reservation.getPaymentStatus());
        order.setPaidAmount(money(reservation.getPaidAmount()).setScale(0, RoundingMode.HALF_UP));
        order.setIsPaid(PaymentStatus.PAID.equals(order.getPaymentStatus())
                || PaymentStatus.OVERPAID.equals(order.getPaymentStatus()));
        Order savedOrder = orderRepository.save(order);

        BigDecimal subTotal = BigDecimal.ZERO;
        for (int index = 0; index < lines.size(); index++) {
            CheckoutLine line = lines.get(index);
            ReservationPreorderItem preorderItem = preorderItems.get(index);
            BigDecimal lineTotal = money(preorderItem.getLineTotal());
            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProduct(line.product());
            detail.setQuantity(line.quantity());
            detail.setPrice(lineTotal);
            detail.setTaxRate(BigDecimal.ZERO);
            detail.setTaxAmount(BigDecimal.ZERO);
            detail.setStatus(0);
            detail.setNote(line.note());
            detail.setPriority(0);
            detail.setQueuedAt(new Date());
            orderDetailRepository.save(detail);
            subTotal = subTotal.add(lineTotal);
        }

        savedOrder.setSubTotal(subTotal);
        savedOrder.setTaxAmount(BigDecimal.ZERO);
        savedOrder.setTotalAmount(subTotal);
        savedOrder.setRemainingAmount(subTotal.subtract(money(reservation.getPaidAmount()))
                .max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP));
        consumeInventory(requirements, lockedBatches);
        orderRepository.save(savedOrder);
        activityLogService.log("CREATE", "Order", String.valueOf(savedOrder.getId()),
                "Chuyển món đặt trước " + reservation.getReservationCode() + " xuống bếp");
        return savedOrder.getId();
    }

    private void validateRequest(OrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Giỏ hàng không được để trống");
        }
    }

    @Transactional
    public AddItemsResult addItems(Integer orderId, OrderRequest request, String idempotencyKey) {
        validateRequest(request);
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        List<RequestedItem> requestedItems = normalizeItems(request.getItems());
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        String requestHash = addItemsRequestHash(orderId, requestedItems);
        var existingOperation = orderItemOperationRepository
                .findByOrderIdAndIdempotencyKey(orderId, normalizedIdempotencyKey);
        if (existingOperation.isPresent()) {
            OrderItemOperation operation = existingOperation.get();
            if (!MessageDigest.isEqual(operation.getRequestHash().getBytes(StandardCharsets.US_ASCII),
                    requestHash.getBytes(StandardCharsets.US_ASCII))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency key has already been used for a different request");
            }
            return new AddItemsResult(orderId, operation.getAddedItems(), operation.getSubTotal(),
                    operation.getTaxAmount(), operation.getTotalAmount());
        }
        if (Boolean.TRUE.equals(order.getIsPaid()) || Integer.valueOf(3).equals(order.getStatus())
                || Integer.valueOf(4).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order cannot accept more items");
        }
        List<CheckoutLine> lines = loadProducts(requestedItems);
        Map<Long, IngredientRequirement> requirements = inventoryRequirements(lines);
        Map<Long, List<IngredientBatch>> lockedBatches = lockAndValidateInventory(requirements);
        BigDecimal subTotal = money(order.getSubTotal());
        BigDecimal taxAmount = money(order.getTaxAmount());
        int addedItems = 0;
        for (CheckoutLine line : lines) {
            Product product = line.product();
            BigDecimal lineSubtotal = money(product.getPrice()).multiply(BigDecimal.valueOf(line.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxRate = decimal(product.getTaxRate(), 8.0);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(line.quantity());
            detail.setPrice(lineSubtotal);
            detail.setTaxRate(taxRate);
            detail.setTaxAmount(lineTax);
            detail.setStatus(0);
            detail.setNote(line.note());
            detail.setAllergyNote(line.allergyNote());
            detail.setPriority(line.priority());
            detail.setQueuedAt(new Date());
            orderDetailRepository.save(detail);
            subTotal = subTotal.add(lineSubtotal);
            taxAmount = taxAmount.add(lineTax);
            addedItems += line.quantity();
        }
        consumeInventory(requirements, lockedBatches);
        BigDecimal totalAmount = subTotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        order.setSubTotal(subTotal);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);
        OrderItemOperation operation = new OrderItemOperation();
        operation.setOrderId(orderId);
        operation.setIdempotencyKey(normalizedIdempotencyKey);
        operation.setRequestHash(requestHash);
        operation.setAddedItems(addedItems);
        operation.setSubTotal(subTotal);
        operation.setTaxAmount(taxAmount);
        operation.setTotalAmount(totalAmount);
        orderItemOperationRepository.save(operation);
        activityLogService.log("UPDATE", "Order", String.valueOf(orderId), "Them mon vao don hang");
        return new AddItemsResult(order.getId(), addedItems, subTotal, taxAmount, totalAmount);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REQUIRED");
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() < 8 || normalized.length() > 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_INVALID");
        }
        return normalized;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(BigDecimal value, double fallback) {
        return value == null ? BigDecimal.valueOf(fallback) : value;
    }

    private String addItemsRequestHash(Integer orderId, List<RequestedItem> items) {
        String payload = items.stream()
                .sorted(Comparator.comparing(RequestedItem::productId)
                        .thenComparing(RequestedItem::note)
                        .thenComparing(RequestedItem::allergyNote)
                        .thenComparing(RequestedItem::priority))
                .map(item -> item.productId() + ":" + item.quantity() + ":" + item.note()
                        + ":" + item.allergyNote() + ":" + item.priority())
                .reduce("ADD_ITEMS|" + orderId, (left, right) -> left + "|" + right);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private List<RequestedItem> normalizeItems(List<OrderDetailRequest> items) {
        Map<Integer, Integer> quantitiesByProduct = new LinkedHashMap<>();
        List<RequestedItem> normalized = new ArrayList<>();
        for (OrderDetailRequest item : items) {
            if (item == null || item.getProductId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Sản phẩm không được để trống");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0 || item.getQuantity() > 100) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số lượng món phải từ 1 đến 100");
            }
            int combined = quantitiesByProduct.getOrDefault(item.getProductId(), 0) + item.getQuantity();
            if (combined > 100) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Tổng số lượng mỗi món không được vượt quá 100");
            }
            quantitiesByProduct.put(item.getProductId(), combined);
            normalized.add(new RequestedItem(item.getProductId(), item.getQuantity(),
                    normalizedText(item.getNote()), normalizedText(item.getAllergyNote()),
                    item.getPriority() == null ? 0 : item.getPriority()));
        }
        return normalized;
    }

    private List<CheckoutLine> loadProducts(List<RequestedItem> requestedItems) {
        List<CheckoutLine> lines = new ArrayList<>();
        for (RequestedItem item : requestedItems) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Sản phẩm không tồn tại: " + item.productId()));
            if (!Boolean.TRUE.equals(product.getStatus()) || !Boolean.TRUE.equals(product.getAvailable())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Món hiện không phục vụ: " + product.getName());
            }
            if (product.getPrice() == null || product.getPrice().signum() < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Giá món chưa hợp lệ: " + product.getName());
            }
            lines.add(new CheckoutLine(product, item.quantity(), item.note(), item.allergyNote(), item.priority()));
        }
        return lines;
    }

    private String normalizedText(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private Account authenticatedAccount(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }
        return accountRepository.findById(username).orElse(null);
    }

    private OrderPaymentOption resolvePaymentOption(OrderPaymentOption requested, boolean dineIn) {
        OrderPaymentOption option = requested == null
                ? (dineIn ? OrderPaymentOption.PAY_AT_RESTAURANT : OrderPaymentOption.PREPAID_TRANSFER)
                : requested;
        if (dineIn && !OrderPaymentOption.PAY_AT_RESTAURANT.equals(option)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Đơn tại quán phải chọn thanh toán tại nhà hàng");
        }
        if (!dineIn && !OrderPaymentOption.PREPAID_TRANSFER.equals(option)
                && !OrderPaymentOption.COD.equals(option)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Đơn giao hàng chỉ hỗ trợ chuyển khoản trước hoặc COD");
        }
        return option;
    }

    /**
     * Validate voucher mà chưa ghi usage (gọi trước khi save order).
     * Trả về Voucher object nếu hợp lệ, null nếu không có voucher.
     */
    private Voucher validateVoucher(String voucherCode, Account account, boolean dineIn) {
        if (dineIn || account == null || voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        Voucher voucher = voucherRepository.findLockedByCode(voucherCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Voucher không tồn tại"));
        if (Boolean.TRUE.equals(voucher.getIsUsed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher đã được sử dụng");
        }
        if (voucher.getAccount() != null
                && !account.getUsername().equals(voucher.getAccount().getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Voucher không thuộc tài khoản này");
        }
        if (voucher.getDiscountPercent() == null
                || voucher.getDiscountPercent() <= 0
                || voucher.getDiscountPercent() > 100) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher có mức giảm không hợp lệ");
        }
        return voucher;
    }

    private double calculateBaseDiscount(Account account, boolean dineIn) {
        if (dineIn || account == null) {
            return 0.0;
        }
        return switch (account.getMembershipTier() == null ? "" : account.getMembershipTier()) {
            case "Kim Cương" -> 0.15;
            case "Vàng" -> 0.10;
            case "Bạc" -> 0.05;
            default -> 0.0;
        };
    }

    private void recordVoucherUsage(Voucher voucher, Order order, Account account, double discountRate) {
        // Tính số tiền giảm thực tế
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (order.getTotalAmount() != null && discountRate > 0) {
            discountAmount = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(discountRate))
                    .setScale(0, RoundingMode.HALF_UP);
        }

        OrderVoucherUsage usage = new OrderVoucherUsage();
        usage.setVoucherId(voucher.getId());
        usage.setVoucherCode(voucher.getCode());
        usage.setOrderId(order.getId());
        usage.setAccountUsername(account != null ? account.getUsername() : null);
        usage.setDiscountAmount(discountAmount);
        usage.setOriginalAmount(order.getTotalAmount());
        usage.setUsedAt(new Date());
        orderVoucherUsageRepository.save(usage);

        // Đánh dấu voucher đã dùng
        voucher.setIsUsed(true);
        voucherRepository.save(voucher);

        activityLogService.log("VOUCHER_USED", "Order", String.valueOf(order.getId()),
                "Áp dụng voucher " + voucher.getCode() + " giảm " + discountAmount.toPlainString() + " VND");
    }

    private Map<Long, IngredientRequirement> inventoryRequirements(List<CheckoutLine> lines) {
        Map<Long, IngredientRequirement> requirements = new LinkedHashMap<>();
        for (CheckoutLine line : lines) {
            List<Recipe> recipes = recipeRepository.findByProduct(line.product());
            if (recipes.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Món chưa được thiết lập nguyên liệu: " + line.product().getName());
            }
            for (Recipe recipe : recipes) {
                Ingredient ingredient = recipe.getIngredient();
                if (ingredient == null || ingredient.getId() == null
                        || recipe.getAmountRequired() == null || recipe.getAmountRequired().signum() <= 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Công thức món chưa hợp lệ: " + line.product().getName());
                }
                BigDecimal amount = recipe.getAmountRequired().multiply(BigDecimal.valueOf(line.quantity()));
                requirements.merge(ingredient.getId(), new IngredientRequirement(ingredient, amount),
                        (left, right) -> new IngredientRequirement(left.ingredient(), left.amount().add(right.amount())));
            }
        }
        return requirements;
    }

    private Map<Long, List<IngredientBatch>> lockAndValidateInventory(
            Map<Long, IngredientRequirement> requirements) {
        Map<Long, List<IngredientBatch>> result = new LinkedHashMap<>();
        Map<String, String> shortages = new LinkedHashMap<>();
        requirements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<IngredientBatch> batches = ingredientBatchRepository
                            .findAvailableBatchesForUpdate(entry.getKey());
                    BigDecimal available = batches.stream()
                            .map(IngredientBatch::getQuantity)
                            .filter(value -> value != null && value.signum() > 0)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    IngredientRequirement requirement = entry.getValue();
                    if (available.compareTo(requirement.amount()) < 0) {
                        shortages.put(requirement.ingredient().getName(),
                                "required=" + requirement.amount() + ", available=" + available);
                    }
                    result.put(entry.getKey(), batches);
                });
        if (!shortages.isEmpty()) {
            throw new InsufficientInventoryException(shortages);
        }
        return result;
    }

    private void consumeInventory(Map<Long, IngredientRequirement> requirements,
                                  Map<Long, List<IngredientBatch>> lockedBatches) {
        for (Map.Entry<Long, IngredientRequirement> entry : requirements.entrySet()) {
            BigDecimal remaining = entry.getValue().amount();
            List<IngredientBatch> batches = lockedBatches.get(entry.getKey());
            for (IngredientBatch batch : batches) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal batchQuantity = batch.getQuantity() == null ? BigDecimal.ZERO : batch.getQuantity();
                BigDecimal consumed = batchQuantity.min(remaining);
                batch.setQuantity(batchQuantity.subtract(consumed));
                remaining = remaining.subtract(consumed);
            }
            ingredientBatchRepository.saveAll(batches);

            BigDecimal quantityAfter = batches.stream()
                    .map(IngredientBatch::getQuantity)
                    .filter(value -> value != null && value.signum() > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Ingredient ingredient = entry.getValue().ingredient();
            ingredient.setQuantity(quantityAfter);
            ingredientRepository.save(ingredient);

            menuAvailabilityService.refreshForIngredient(ingredient);
        }
    }

    private void markTablePending(Order order, String orderCode, RestaurantTable table) {
        if (table.getIsOccupied() != null && table.getIsOccupied() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn đã có đơn đang hoạt động");
        }
        table.setIsOccupied(1);
        table.setReservedTime("Đơn chờ xác nhận: #" + orderCode);
        tableRepository.save(table);
        order.setTableId(table.getId());
    }

    private String safeAddress(String address) {
        if (address == null || address.isBlank()) {
            return "Không cung cấp địa chỉ";
        }
        return address.trim();
    }

    private record RequestedItem(int productId, int quantity, String note, String allergyNote, int priority) {
    }

    private record CheckoutLine(Product product, int quantity, String note, String allergyNote, int priority) {
    }

    private record IngredientRequirement(Ingredient ingredient, BigDecimal amount) {
    }

    public record CheckoutResult(Integer orderId,
                                 String orderCode,
                                 Integer status,
                                 BigDecimal subTotal,
                                 BigDecimal taxAmount,
                                 BigDecimal totalAmount,
                                 OrderPaymentOption paymentOption,
                                 PaymentStatus paymentStatus,
                                 PaymentQrResponse payment) {
    }

    public record AddItemsResult(Integer orderId, int addedItems, BigDecimal subTotal,
                                 BigDecimal taxAmount, BigDecimal totalAmount) {
    }
}
