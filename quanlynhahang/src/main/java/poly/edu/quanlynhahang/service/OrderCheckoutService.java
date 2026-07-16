package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OrderCheckoutService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final RestaurantTableRepository tableRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final VoucherRepository voucherRepository;
    private final ActivityLogService activityLogService;
    private final OrderPaymentService orderPaymentService;

    public OrderCheckoutService(OrderRepository orderRepository,
                                OrderDetailRepository orderDetailRepository,
                                ProductRepository productRepository,
                                AccountRepository accountRepository,
                                RestaurantTableRepository tableRepository,
                                RecipeRepository recipeRepository,
                                IngredientRepository ingredientRepository,
                                IngredientBatchRepository ingredientBatchRepository,
                                VoucherRepository voucherRepository,
                                ActivityLogService activityLogService,
                                OrderPaymentService orderPaymentService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
        this.tableRepository = tableRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.voucherRepository = voucherRepository;
        this.activityLogService = activityLogService;
        this.orderPaymentService = orderPaymentService;
    }

    @Transactional
    public CheckoutResult checkout(OrderRequest request, String username) {
        validateRequest(request);
        Account account = authenticatedAccount(username);
        boolean dineIn = request.getAddress() != null && request.getAddress().contains("[TẠI QUÁN]");
        OrderPaymentOption paymentOption = resolvePaymentOption(request.getPaymentOption(), dineIn);

        Map<Integer, Integer> quantities = normalizeQuantities(request.getItems());
        List<CheckoutLine> lines = loadProducts(quantities);
        double discountRate = calculateDiscount(request.getVoucherCode(), account, dineIn);
        Map<Long, IngredientRequirement> requirements = inventoryRequirements(lines);
        Map<Long, List<IngredientBatch>> lockedBatches = lockAndValidateInventory(requirements);

        String orderCode = String.format(Locale.ROOT, "%04d", SECURE_RANDOM.nextInt(10_000));
        Order order = new Order();
        order.setAccount(account);
        order.setAddress("MÃ ĐƠN: #" + orderCode + " | " + safeAddress(request.getAddress()));
        order.setCreateDate(new Date());
        order.setStatus(0);
        order.setIsPaid(false);
        order.setDeposit(0.0);
        order.setPaymentOption(paymentOption);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPaidAmount(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        double subTotal = 0.0;
        double taxAmount = 0.0;
        for (CheckoutLine line : lines) {
            Product product = line.product();
            double lineSubtotal = product.getPrice() * line.quantity() * (1.0 - discountRate);
            double taxRate = product.getTaxRate() == null ? 8.0 : product.getTaxRate();
            double lineTax = lineSubtotal * taxRate / 100.0;

            OrderDetail detail = new OrderDetail();
            detail.setOrder(savedOrder);
            detail.setProduct(product);
            detail.setQuantity(line.quantity());
            detail.setPrice(lineSubtotal);
            detail.setTaxRate(taxRate);
            detail.setTaxAmount(lineTax);
            detail.setStatus(0);
            orderDetailRepository.save(detail);
            subTotal += lineSubtotal;
            taxAmount += lineTax;
        }

        savedOrder.setSubTotal(subTotal);
        savedOrder.setTaxAmount(taxAmount);
        savedOrder.setTotalAmount(subTotal + taxAmount);
        savedOrder.setRemainingAmount(BigDecimal.valueOf(savedOrder.getTotalAmount())
                .setScale(0, RoundingMode.HALF_UP));
        consumeInventory(requirements, lockedBatches);
        occupyDineInTable(savedOrder, request.getAddress(), orderCode, dineIn);
        orderRepository.save(savedOrder);
        PaymentQrResponse payment = orderPaymentService.createForOrder(savedOrder);
        activityLogService.log("CREATE", "Order", String.valueOf(savedOrder.getId()),
                "Tạo đơn chờ xác nhận #" + orderCode);

        return new CheckoutResult(savedOrder.getId(), orderCode, savedOrder.getStatus(),
                savedOrder.getSubTotal(), savedOrder.getTaxAmount(), savedOrder.getTotalAmount(),
                savedOrder.getPaymentOption(), savedOrder.getPaymentStatus(), payment);
    }

    private void validateRequest(OrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Giỏ hàng không được để trống");
        }
    }

    private Map<Integer, Integer> normalizeQuantities(List<OrderDetailRequest> items) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (OrderDetailRequest item : items) {
            if (item == null || item.getProductId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Sản phẩm không được để trống");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0 || item.getQuantity() > 100) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số lượng món phải từ 1 đến 100");
            }
            int combined = quantities.getOrDefault(item.getProductId(), 0) + item.getQuantity();
            if (combined > 100) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Tổng số lượng mỗi món không được vượt quá 100");
            }
            quantities.put(item.getProductId(), combined);
        }
        return quantities;
    }

    private List<CheckoutLine> loadProducts(Map<Integer, Integer> quantities) {
        List<CheckoutLine> lines = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Sản phẩm không tồn tại: " + entry.getKey()));
            if (!Boolean.TRUE.equals(product.getStatus()) || !Boolean.TRUE.equals(product.getAvailable())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Món hiện không phục vụ: " + product.getName());
            }
            if (product.getPrice() == null || product.getPrice() < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Giá món chưa hợp lệ: " + product.getName());
            }
            lines.add(new CheckoutLine(product, entry.getValue()));
        }
        return lines;
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

    private double calculateDiscount(String voucherCode, Account account, boolean dineIn) {
        if (dineIn || account == null) {
            if (voucherCode != null && !voucherCode.isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Voucher chỉ áp dụng cho tài khoản đã đăng nhập và đơn mang đi/giao hàng");
            }
            return 0.0;
        }

        double discount = switch (account.getMembershipTier() == null ? "" : account.getMembershipTier()) {
            case "Kim Cương" -> 0.15;
            case "Vàng" -> 0.10;
            case "Bạc" -> 0.05;
            default -> 0.0;
        };
        if (voucherCode != null && !voucherCode.isBlank()) {
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
            discount += voucher.getDiscountPercent() / 100.0;
            voucher.setIsUsed(true);
            voucherRepository.save(voucher);
        }
        return Math.min(discount, 1.0);
    }

    private Map<Long, IngredientRequirement> inventoryRequirements(List<CheckoutLine> lines) {
        Map<Long, IngredientRequirement> requirements = new LinkedHashMap<>();
        for (CheckoutLine line : lines) {
            for (Recipe recipe : recipeRepository.findByProduct(line.product())) {
                Ingredient ingredient = recipe.getIngredient();
                if (ingredient == null || ingredient.getId() == null
                        || recipe.getAmountRequired() == null || recipe.getAmountRequired() <= 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Công thức món chưa hợp lệ: " + line.product().getName());
                }
                double amount = recipe.getAmountRequired() * line.quantity();
                requirements.merge(ingredient.getId(), new IngredientRequirement(ingredient, amount),
                        (left, right) -> new IngredientRequirement(left.ingredient(), left.amount() + right.amount()));
            }
        }
        return requirements;
    }

    private Map<Long, List<IngredientBatch>> lockAndValidateInventory(
            Map<Long, IngredientRequirement> requirements) {
        Map<Long, List<IngredientBatch>> result = new LinkedHashMap<>();
        requirements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<IngredientBatch> batches = ingredientBatchRepository
                            .findAvailableBatchesForUpdate(entry.getKey());
                    double available = batches.stream()
                            .map(IngredientBatch::getQuantity)
                            .filter(value -> value != null && value > 0)
                            .mapToDouble(Double::doubleValue)
                            .sum();
                    if (available + 0.000001 < entry.getValue().amount()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Không đủ nguyên liệu: " + entry.getValue().ingredient().getName());
                    }
                    result.put(entry.getKey(), batches);
                });
        return result;
    }

    private void consumeInventory(Map<Long, IngredientRequirement> requirements,
                                  Map<Long, List<IngredientBatch>> lockedBatches) {
        for (Map.Entry<Long, IngredientRequirement> entry : requirements.entrySet()) {
            double remaining = entry.getValue().amount();
            List<IngredientBatch> batches = lockedBatches.get(entry.getKey());
            for (IngredientBatch batch : batches) {
                if (remaining <= 0) {
                    break;
                }
                double batchQuantity = batch.getQuantity() == null ? 0.0 : batch.getQuantity();
                double consumed = Math.min(batchQuantity, remaining);
                batch.setQuantity(batchQuantity - consumed);
                remaining -= consumed;
            }
            ingredientBatchRepository.saveAll(batches);

            double quantityAfter = batches.stream()
                    .map(IngredientBatch::getQuantity)
                    .filter(value -> value != null && value > 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            Ingredient ingredient = entry.getValue().ingredient();
            ingredient.setQuantity(quantityAfter);
            ingredientRepository.save(ingredient);

            for (Recipe relatedRecipe : recipeRepository.findByIngredient(ingredient)) {
                if (relatedRecipe.getProduct() != null
                        && relatedRecipe.getAmountRequired() != null
                        && quantityAfter + 0.000001 < relatedRecipe.getAmountRequired()) {
                    relatedRecipe.getProduct().setAvailable(false);
                    productRepository.save(relatedRecipe.getProduct());
                }
            }
        }
    }

    private void occupyDineInTable(Order order, String address, String orderCode, boolean dineIn) {
        if (!dineIn || address == null) {
            return;
        }
        tableRepository.findAll().stream()
                .filter(table -> table.getName() != null && address.contains(table.getName()))
                .max(Comparator.comparingInt(table -> table.getName().length()))
                .ifPresent(table -> markTablePending(order, orderCode, table));
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

    private record CheckoutLine(Product product, int quantity) {
    }

    private record IngredientRequirement(Ingredient ingredient, double amount) {
    }

    public record CheckoutResult(Integer orderId,
                                 String orderCode,
                                 Integer status,
                                 Double subTotal,
                                 Double taxAmount,
                                 Double totalAmount,
                                 OrderPaymentOption paymentOption,
                                 PaymentStatus paymentStatus,
                                 PaymentQrResponse payment) {
    }
}
