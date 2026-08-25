package poly.edu.quanlynhahang.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Map;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.KitchenDishCancelRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.dto.OrderResponse;
import poly.edu.quanlynhahang.dto.GuestBookingRequest;
import poly.edu.quanlynhahang.dto.MergeTablesRequest;
import poly.edu.quanlynhahang.dto.SplitTableRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.service.OrderCheckoutService;
import poly.edu.quanlynhahang.service.KitchenOrderDetailService;
import poly.edu.quanlynhahang.service.CustomerInvoiceEmailService;
import poly.edu.quanlynhahang.service.TableSessionService;
import poly.edu.quanlynhahang.service.OrderStateMachineService;
import poly.edu.quanlynhahang.service.OrderFinancialMutationGuardService;
import poly.edu.quanlynhahang.service.TableLifecycleService;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.OrderType;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private RestaurantTableRepository tableRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IngredientBatchRepository ingredientBatchRepository;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private OrderCheckoutService orderCheckoutService;
    @Autowired private KitchenOrderDetailService kitchenOrderDetailService;
    @Autowired private CustomerInvoiceEmailService customerInvoiceEmailService;
    @Autowired private TableSessionService tableSessionService;
    @Autowired private OrderStateMachineService orderStateMachineService;
    @Autowired private OrderFinancialMutationGuardService orderFinancialMutationGuardService;
    @Autowired private TableLifecycleService tableLifecycleService;

    @GetMapping("/history")
    public ResponseEntity<?> getMyOrders() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderRepository.findByAccountUsername(currentUsername).stream()
                .map(OrderResponse::from).toList());
    }

    @GetMapping("/open-by-table")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getOpenDineInOrder(@org.springframework.web.bind.annotation.RequestParam Integer tableId) {
        return orderRepository.findOpenDineInOrdersByTableIdWithDetails(tableId).stream()
                .findFirst()
                .map(order -> ResponseEntity.ok(OrderResponse.from(order)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * DEPRECATED P0-04: Guest booking endpoint removed.
     * Use POST /api/reservations instead via ReservationService.createReservation().
     * This endpoint now returns 410 Gone with a redirect instruction.
     */
    @Deprecated
    @PostMapping("/guest-booking")
    public ResponseEntity<?> guestBooking(@Valid @RequestBody GuestBookingRequest payload) {
        log.warn("Deprecated guest-booking endpoint called. Please migrate to POST /api/reservations");
        return ResponseEntity.status(HttpStatus.GONE)
                .body(java.util.Map.of(
                        "error", "GONE",
                        "message", "Endpoint đặt bàn khách vãng lai đã bị xoá. Vui lòng sử dụng POST /api/reservations.",
                        "migrationHint", "Dùng ReservationService.createReservation() thay thế."
                ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody OrderRequest orderRequest,
                                      @RequestHeader(value = "X-Table-Session-Token", required = false) String tableSessionToken,
                                      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        if (OrderType.DINE_IN.equals(orderRequest.getOrderType())) {
            if (tableSessionToken != null && !tableSessionToken.isBlank()) {
                tableSessionService.requireForTable(tableSessionToken, orderRequest.getTableId());
            }
        }
        String username = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
        var result = orderCheckoutService.checkout(orderRequest, username, idempotencyKey);
        if (OrderType.DINE_IN.equals(orderRequest.getOrderType())) {
            publishSafely("/topic/kitchen", "NEW_ORDER");
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/add-items")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> addItemsToOrder(@PathVariable Integer id, @Valid @RequestBody OrderRequest orderRequest,
                                              @RequestHeader("X-Idempotency-Key") String idempotencyKey) {
        var result = orderCheckoutService.addItems(id, orderRequest, idempotencyKey);
        publishSafely("/topic/kitchen", "NEW_ORDER");
        publishSafely("/topic/waiter", "DISH_STATUS_CHANGED");
        return ResponseEntity.ok(result);
    }

    private void publishSafely(String destination, String event) {
        try {
            messagingTemplate.convertAndSend(destination, event);
        } catch (Exception exception) {
            log.warn("Order update succeeded but WebSocket broadcast to {} failed: {}", destination,
                    exception.getMessage(), exception);
        }
    }

    @PostMapping("/{id}/invoice-request")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> requestInvoice(@PathVariable Integer id,
                                             @RequestParam(required = false) String email) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Order order = orderRepository.findLockedById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy hóa đơn."));
        if (order.getAccount() == null || !username.equals(order.getAccount().getUsername())) {
            return ResponseEntity.status(403).body("Bạn không có quyền yêu cầu hóa đơn này.");
        }
        if (!Boolean.TRUE.equals(order.getIsPaid())) {
            return ResponseEntity.status(409).body("Chỉ có thể yêu cầu xuất hóa đơn sau khi thanh toán đủ.");
        }
        String destination = email == null || email.isBlank() ? order.getAccount().getEmail() : email.trim();
        if (destination == null || !destination.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body("Email nhận hóa đơn không hợp lệ.");
        }
        order.setInvoiceRequested(true);
        order.setInvoiceRequestedAt(new Date());
        order.setInvoiceEmail(destination);
        orderRepository.save(order);
        CustomerInvoiceEmailService.DeliveryStatus deliveryStatus =
                customerInvoiceEmailService.sendPaidInvoiceNotice(order, destination);
        String message = deliveryStatus == CustomerInvoiceEmailService.DeliveryStatus.SENT
                ? "Đã gửi xác nhận yêu cầu xuất hóa đơn qua email."
                : "Đã ghi nhận yêu cầu xuất hóa đơn.";
        return ResponseEntity.ok(Map.of(
                "message", message,
                "email", destination,
                "emailSent", deliveryStatus == CustomerInvoiceEmailService.DeliveryStatus.SENT));
    }

    @PostMapping("/merge-tables")
    @Transactional
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> mergeTables(@Valid @RequestBody MergeTablesRequest payload) {
        if (payload.fromTableId().equals(payload.toTableId())) {
            return ResponseEntity.badRequest().body("Source and destination tables must differ.");
        }
        Map<Integer, RestaurantTable> lockedTables = lockTables(payload.fromTableId(), payload.toTableId());
        RestaurantTable fromTable = lockedTables.get(payload.fromTableId());
        RestaurantTable toTable = lockedTables.get(payload.toTableId());
        if (Boolean.FALSE.equals(toTable.getActive())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Bàn đích đang ngừng hoạt động");
        }

        Optional<Order> sourceOrderOpt = orderRepository
                .findOpenDineInOrdersByTableIdWithDetails(fromTable.getId()).stream().findFirst();
        Optional<Order> targetOrderOpt = orderRepository
                .findOpenDineInOrdersByTableIdWithDetails(toTable.getId()).stream().findFirst();
            
        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn nguồn không có hóa đơn nào đang mở!");
        }
        if (targetOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn đích không có hóa đơn nào đang mở! Vui lòng order món cho bàn đích trước.");
        }
        
        Map<Integer, Order> lockedOrders = lockOrders(
                sourceOrderOpt.get().getId(), targetOrderOpt.get().getId());
        Order sourceOrder = lockedOrders.get(sourceOrderOpt.get().getId());
        Order targetOrder = lockedOrders.get(targetOrderOpt.get().getId());
        orderFinancialMutationGuardService.requireSafeForTableComposition(sourceOrder, targetOrder);
        
        // Chuyển toàn bộ món từ hóa đơn cũ sang hóa đơn mới
        BigDecimal transferSub = BigDecimal.ZERO;
        BigDecimal transferTax = BigDecimal.ZERO;
        if (sourceOrder.getOrderDetails() != null) {
            for (OrderDetail detail : sourceOrder.getOrderDetails()) {
                detail.setOrder(targetOrder);
                orderDetailRepository.save(detail);
                transferSub = transferSub.add(money(detail.getPrice()));
                transferTax = transferTax.add(money(detail.getTaxAmount()));
            }
        }
        
        BigDecimal targetSubTotal = money(targetOrder.getSubTotal()).add(transferSub);
        BigDecimal targetTaxAmount = money(targetOrder.getTaxAmount()).add(transferTax);
        targetOrder.setSubTotal(targetSubTotal);
        targetOrder.setTaxAmount(targetTaxAmount);
        targetOrder.setTotalAmount(targetSubTotal.add(targetTaxAmount));
        targetOrder.setRemainingAmount(targetOrder.getTotalAmount().setScale(0, RoundingMode.HALF_UP));
        orderRepository.save(targetOrder);
        
        // Hủy hóa đơn cũ
        sourceOrder.setSubTotal(BigDecimal.ZERO.setScale(2));
        sourceOrder.setTaxAmount(BigDecimal.ZERO.setScale(2));
        sourceOrder.setTotalAmount(BigDecimal.ZERO.setScale(2));
        sourceOrder.setRemainingAmount(BigDecimal.ZERO);
        orderStateMachineService.transition(sourceOrder, OrderStatus.CANCELLED);
        orderRepository.save(sourceOrder);
        
        // Đánh dấu bàn cũ là Đã Ghép thay vì Trống
        fromTable.setIsOccupied(5);
        fromTable.setReservedTime("[GHÉP VỚI: " + toTable.getName() + "]");
        tableRepository.save(fromTable);
        tableSessionService.revokeActiveForTable(fromTable.getId());
            
        messagingTemplate.convertAndSend("/topic/orders", "TABLE_MERGED");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Gộp bàn thành công!"));
    }

    @PostMapping("/split-table")
    @Transactional
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> splitTable(@Valid @RequestBody SplitTableRequest payload) {
        if (payload.fromTableId().equals(payload.toTableId())) {
            return ResponseEntity.badRequest().body("Source and destination tables must differ.");
        }
        Map<Integer, RestaurantTable> lockedTables = lockTables(payload.fromTableId(), payload.toTableId());
        RestaurantTable fromTable = lockedTables.get(payload.fromTableId());
        RestaurantTable toTable = lockedTables.get(payload.toTableId());
        if (Boolean.FALSE.equals(toTable.getActive())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Bàn đích đang ngừng hoạt động");
        }
        List<Integer> detailIds = payload.detailIds();

        Optional<Order> sourceOrderOpt = orderRepository
                .findOpenDineInOrdersByTableIdWithDetails(fromTable.getId()).stream().findFirst();

        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn nguồn không có hóa đơn nào đang mở!");
        }

        Order sourceOrder = orderRepository.findLockedById(sourceOrderOpt.get().getId()).orElseThrow();
        
        // Cố gắng tìm Order của bàn đích
        Optional<Order> targetOrderOpt = orderRepository
                .findOpenDineInOrdersByTableIdWithDetails(toTable.getId()).stream().findFirst();
        
        if (targetOrderOpt.isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Bàn đích đang có đơn mở, không thể tách thêm vào bàn này");
        }
        // Tạo Order mới cho bàn đích
        String uniqueOrderCode = generateUnique4DigitCode();
        Order targetOrder = new Order();
        targetOrder.setOrderCode("ORD-" + java.util.UUID.randomUUID().toString()
                .substring(0, 12).toUpperCase(java.util.Locale.ROOT));
        targetOrder.setAccount(sourceOrder.getAccount()); // copy account
        targetOrder.setTableId(toTable.getId());
        targetOrder.setOrderType(poly.edu.quanlynhahang.entity.OrderType.DINE_IN);
        targetOrder.setAddress(null);
        targetOrder.setCreateDate(new Date());
        orderStateMachineService.initializeFrom(targetOrder, sourceOrder);
        targetOrder = orderRepository.save(targetOrder);

        // Cập nhật trạng thái bàn đích
        final String fUniqueOrderCode = uniqueOrderCode;
        toTable.setIsOccupied(2);
        toTable.setReservedTime("Đơn: #" + fUniqueOrderCode);
        tableRepository.save(toTable);
        orderFinancialMutationGuardService.requireSafeForTableComposition(sourceOrder, targetOrder);

        java.util.Set<Integer> uniqueDetailIds = new java.util.LinkedHashSet<>(detailIds);
        if (uniqueDetailIds.size() != detailIds.size()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Danh sách món tách bàn có ID trùng lặp");
        }
        List<OrderDetail> selectedDetails = orderDetailRepository.findAllById(uniqueDetailIds);
        if (selectedDetails.size() != uniqueDetailIds.size()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Danh sách món tách bàn chứa ID không tồn tại");
        }
        if (selectedDetails.stream().anyMatch(detail -> detail.getOrder() == null
                || !sourceOrder.getId().equals(detail.getOrder().getId()))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Tất cả món được chọn phải thuộc đơn của bàn nguồn");
        }
        
        final Order finalTargetOrder = targetOrder;
        BigDecimal moveSub = BigDecimal.ZERO;
        BigDecimal moveTax = BigDecimal.ZERO;
        // Di chuyển các order detail
        for (OrderDetail detail : selectedDetails) {
            detail.setOrder(finalTargetOrder);
            orderDetailRepository.save(detail);
            moveSub = moveSub.add(money(detail.getPrice()));
            moveTax = moveTax.add(money(detail.getTaxAmount()));
        }
        
        BigDecimal sourceSubTotal = money(sourceOrder.getSubTotal()).subtract(moveSub).max(BigDecimal.ZERO);
        BigDecimal sourceTaxAmount = money(sourceOrder.getTaxAmount()).subtract(moveTax).max(BigDecimal.ZERO);
        sourceOrder.setSubTotal(sourceSubTotal);
        sourceOrder.setTaxAmount(sourceTaxAmount);
        sourceOrder.setTotalAmount(sourceSubTotal.add(sourceTaxAmount));
        sourceOrder.setRemainingAmount(sourceOrder.getTotalAmount().setScale(0, RoundingMode.HALF_UP));
        orderRepository.save(sourceOrder);
        
        BigDecimal finalTargetSubTotal = money(finalTargetOrder.getSubTotal()).add(moveSub);
        BigDecimal finalTargetTaxAmount = money(finalTargetOrder.getTaxAmount()).add(moveTax);
        finalTargetOrder.setSubTotal(finalTargetSubTotal);
        finalTargetOrder.setTaxAmount(finalTargetTaxAmount);
        finalTargetOrder.setTotalAmount(finalTargetSubTotal.add(finalTargetTaxAmount));
        finalTargetOrder.setRemainingAmount(finalTargetOrder.getTotalAmount().setScale(0, RoundingMode.HALF_UP));
        orderRepository.save(finalTargetOrder);

        // Nếu bàn nguồn không còn OrderDetail nào, thì Hủy order đó và giải phóng bàn
        long remainingItems = orderDetailRepository.countByOrderId(sourceOrder.getId());
            
        if (remainingItems == 0) {
            orderStateMachineService.transition(sourceOrder, OrderStatus.CANCELLED);
            orderRepository.save(sourceOrder);
            tableLifecycleService.release(fromTable.getId());
        }

        messagingTemplate.convertAndSend("/topic/orders", "TABLE_SPLIT");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Tách bàn thành công!"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/details/{detailId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or (hasRole('KITCHEN') and #status == 1) or (hasRole('WAITER') and #status == 2)")
    public ResponseEntity<?> updateOrderDetailStatus(@PathVariable Integer detailId, @org.springframework.web.bind.annotation.RequestParam Integer status) {
        if (status == null || status < 0 || status > 2) {
            return ResponseEntity.badRequest().body("Invalid dish status.");
        }
        if (status == 1) {
            kitchenOrderDetailService.complete(detailId);
        } else if (status == 2) {
            kitchenOrderDetailService.serve(detailId);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Không được đưa món quay lại trạng thái chờ qua endpoint này.");
        }
        return ResponseEntity.ok("Cập nhật món thành công!");
    }

    private String generateSecureOrderCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // exclude I, O, 0, 1 to avoid confusion
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString().toUpperCase();
    }

    private String generateUnique4DigitCode() {
        int code = 1000 + SECURE_RANDOM.nextInt(9000);
        return String.valueOf(code);
    }

    @PutMapping("/details/{detailId}/kitchen/start")
    @PreAuthorize("hasAnyRole('KITCHEN', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> startKitchenDish(@PathVariable Integer detailId) {
        return ResponseEntity.ok(poly.edu.quanlynhahang.dto.OrderDetailResponse
                .from(kitchenOrderDetailService.start(detailId)));
    }

    @PutMapping("/details/{detailId}/kitchen/complete")
    @PreAuthorize("hasAnyRole('KITCHEN', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> completeKitchenDish(@PathVariable Integer detailId) {
        return ResponseEntity.ok(poly.edu.quanlynhahang.dto.OrderDetailResponse
                .from(kitchenOrderDetailService.complete(detailId)));
    }

    @PutMapping("/details/{detailId}/kitchen/cancel")
    @PreAuthorize("hasAnyRole('KITCHEN', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> cancelKitchenDish(@PathVariable Integer detailId,
                                                @Valid @RequestBody KitchenDishCancelRequest request,
                                                org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(poly.edu.quanlynhahang.dto.OrderDetailResponse
                .from(kitchenOrderDetailService.cancel(detailId, request.reason(), authentication.getName())));
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Integer, RestaurantTable> lockTables(Integer firstId, Integer secondId) {
        List<RestaurantTable> tables = tableRepository.findLockedByIdIn(List.of(firstId, secondId));
        if (tables.size() != 2) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy bàn nguồn hoặc bàn đích.");
        }
        return tables.stream().collect(java.util.stream.Collectors.toMap(RestaurantTable::getId, table -> table));
    }

    private Map<Integer, Order> lockOrders(Integer firstId, Integer secondId) {
        List<Order> orders = orderRepository.findLockedByIdIn(List.of(firstId, secondId));
        if (orders.size() != 2) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Đơn tại bàn đã thay đổi, vui lòng tải lại.");
        }
        return orders.stream().collect(java.util.stream.Collectors.toMap(Order::getId, order -> order));
    }
}
