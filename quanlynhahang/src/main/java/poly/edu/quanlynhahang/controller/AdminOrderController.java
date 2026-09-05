package poly.edu.quanlynhahang.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.Valid;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.dto.OrderResponse;
import poly.edu.quanlynhahang.dto.RefundCompletionRequest;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.PointsLedgerService;
import poly.edu.quanlynhahang.service.OrderPaymentService;
import poly.edu.quanlynhahang.service.InventoryReservationService;
import poly.edu.quanlynhahang.service.OrderStateMachineService;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.service.OrderRefundService;
import poly.edu.quanlynhahang.service.TableSessionService;
import poly.edu.quanlynhahang.service.TableLifecycleService;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.service.OrderServiceDateGuardService;
@RestController
@RequestMapping("/api/admin/orders")
// ✅ FIX: Dùng hasAnyAuthority với ROLE_ prefix đầy đủ
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
public class AdminOrderController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private PointsLedgerService pointsLedgerService;

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private InventoryReservationService inventoryReservationService;

    @Autowired
    private OrderRefundService orderRefundService;

    @Autowired
    private OrderStateMachineService orderStateMachineService;

    @Autowired
    private TableSessionService tableSessionService;

    @Autowired
    private TableLifecycleService tableLifecycleService;

    @Autowired
    private OrderServiceDateGuardService serviceDateGuard;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllOrders(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "200") int limit) {
        if (page != null) {
            int safePage = Math.max(0, page);
            int safeSize = Math.max(1, Math.min(size, 100));
            PageRequest pageRequest = PageRequest.of(safePage, safeSize);
            List<Integer> ids = orderRepository.findRecentOrderIds(pageRequest);
            List<OrderResponse> orders = (ids.isEmpty() ? List.<Order>of() : orderRepository.findAllWithDetailsByIdIn(ids)).stream()
                    .sorted((o1, o2) -> o2.getId().compareTo(o1.getId()))
                    .map(OrderResponse::from)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new PageImpl<>(orders, pageRequest, orderRepository.count()));
        }

        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<Integer> ids = orderRepository.findRecentOrderIds(PageRequest.of(0, safeLimit));
        List<Order> orders = (ids.isEmpty() ? List.<Order>of() : orderRepository.findAllWithDetailsByIdIn(ids)).stream()
                .sorted((o1, o2) -> o2.getId().compareTo(o1.getId()))
                .collect(Collectors.toList());
        // This shared staff endpoint is the canonical order snapshot for Waiter,
        // Cashier and table management. Kitchen has its own filtered /kitchen/board.
        return ResponseEntity.ok(orders.stream().map(OrderResponse::from).toList());
    }

    @GetMapping("/kitchen/board")
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getKitchenBoard() {
        Date startOfDay = Date.from(LocalDate.now(BUSINESS_ZONE).atStartOfDay(BUSINESS_ZONE).toInstant());
        List<Order> orders = orderRepository.findKitchenBoardOrdersWithDetails(
                OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED,
                List.of(0), List.of(1, 2),
                startOfDay);
        return ResponseEntity.ok(orders.stream()
                .filter(order -> serviceDateGuard == null || serviceDateGuard.isPreparationReached(order))
                .map(order -> serviceDateGuard == null ? OrderResponse.from(order)
                        : OrderResponse.from(order, serviceDateGuard.resolveTiming(order))).toList());
    }

    // THỐNG KÊ (Khóa lại chỉ cho Quản lý xem)
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRevenueAnalytics() {
        List<Order> revenueOrders = orderRepository.findOrdersForRevenueAnalytics();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalItemsSold = 0;
        for (Order order : revenueOrders) {
            boolean completedLifecycle = order.getStatus() == OrderStatus.COMPLETED.code()
                    || order.getStatus() == OrderStatus.SERVED.code();
            if (order.getOrderDetails() != null && isRevenueOrder(order)) {
                totalRevenue = totalRevenue.add(orderTotal(order));
                if (completedLifecycle) {
                    totalItemsSold += order.getOrderDetails().stream()
                            .filter(detail -> !Integer.valueOf(3).equals(detail.getStatus()))
                            .mapToInt(d -> d.getQuantity()).sum();
                }
            }
        }
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRevenue", totalRevenue);
        long completedInvoiceCount = revenueOrders.stream()
                .filter(this::isRevenueOrder)
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED.code()
                        || order.getStatus() == OrderStatus.SERVED.code())
                .count();
        statistics.put("completedOrdersCount", completedInvoiceCount);
        statistics.put("totalItemsSold", totalItemsSold);
        statistics.put("pendingOrdersCount", orderRepository.countByStatus(OrderStatus.PENDING.code()));
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardStats() {
        List<Order> completedOrders = orderRepository.findOrdersForRevenueAnalytics();

        // 1. Doanh thu 7 ngày qua
        Map<String, BigDecimal> revenueByDate = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        for (Order o : completedOrders) {
            if (isRevenueOrder(o) && o.getCreateDate() != null && o.getOrderDetails() != null) {
                String dateStr = sdf.format(o.getCreateDate());
                revenueByDate.merge(dateStr, orderTotal(o), BigDecimal::add);
            }
        }

        // 2. Top 5 sản phẩm bán chạy
        Map<String, Integer> productSales = new HashMap<>();
        for (Order o : completedOrders) {
            if (isRevenueOrder(o) && o.getOrderDetails() != null) {
                for (poly.edu.quanlynhahang.entity.OrderDetail d : o.getOrderDetails()) {
                    if (d.getProduct() != null) {
                        String pName = d.getProduct().getName();
                        productSales.put(pName, productSales.getOrDefault(pName, 0) + d.getQuantity());
                    }
                }
            }
        }
        List<Map.Entry<String, Integer>> topProducts = productSales.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("revenueChart", revenueByDate);
        stats.put("topProducts", topProducts.stream().map(e -> Map.of("name", e.getKey(), "sold", e.getValue())).collect(Collectors.toList()));
        LocalDateTime overdueCutoff = LocalDate.now(BUSINESS_ZONE).atStartOfDay();
        stats.put("overdueOrders", orderRepository.countOverdueOrders(
                List.of(OrderStatus.CANCELLED.code(), OrderStatus.COMPLETED.code()),
                overdueCutoff,
                Date.from(overdueCutoff.atZone(BUSINESS_ZONE).toInstant())));

        return ResponseEntity.ok(stats);
    }

    // 🌟 API MỚI: XỬ LÝ NÚT BẤM "XONG MÓN" HOẶC "ĐÃ BƯNG RA BÀN"
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or (hasRole('KITCHEN') and (#status == 2 or #status == 6)) or (hasRole('WAITER') and (#status == 1 or #status == 7))")
    @Transactional
    public ResponseEntity<?> updateOrderStatus(@PathVariable Integer id, @RequestParam Integer status) {
        if (status == 1) {
            Order current = orderRepository.findById(id).orElse(null);
            if (current != null && (Integer.valueOf(0).equals(current.getStatus())
                    || Integer.valueOf(5).equals(current.getStatus()))) {
                requireManualConfirmationRole();
                return ResponseEntity.ok(orderPaymentService.confirmManualDispatch(id));
            }
        }
        return orderRepository.findById(id).map(order -> {
            if (status == OrderStatus.PARTIALLY_READY.code()
                    || status == OrderStatus.READY.code()
                    || status == OrderStatus.SERVED.code()) {
                if (serviceDateGuard != null) serviceDateGuard.assertPreparationReached(order);
            }
            boolean shouldAwardPoints = status == OrderStatus.COMPLETED.code()
                    && order.getStatus() != OrderStatus.COMPLETED.code()
                    && Boolean.TRUE.equals(order.getIsPaid());
            if (status == OrderStatus.PARTIALLY_READY.code() && order.getOrderDetails() != null) {
                Date startedAt = new Date();
                order.getOrderDetails().stream()
                        .filter(detail -> !Integer.valueOf(3).equals(detail.getStatus()))
                        .filter(detail -> detail.getStartedAt() == null)
                        .forEach(detail -> detail.setStartedAt(startedAt));
            }
            if (status == 7 && order.getOrderDetails() != null) {
                order.getOrderDetails().stream()
                        .filter(detail -> Integer.valueOf(1).equals(detail.getStatus()))
                        .forEach(detail -> detail.setStatus(2));
            }
            orderStateMachineService.transition(order, status);
            orderRepository.save(order);
            if (shouldAwardPoints) {
                awardOrderPoints(order);
            }
            
            String statusText = OrderStatus.fromCode(status).name();
            activityLogService.log("UPDATE", "Order", String.valueOf(id),
                    "Cập nhật trạng thái đơn #" + id + " → " + statusText);
            
            if (status == 1 || status == 6) {
                messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
            } else if (status == 2) {
                messagingTemplate.convertAndSend("/topic/waiter", "ORDER_READY");
            }
            
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!"));
    }

    @PutMapping("/{id}/confirm-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Transactional
    public ResponseEntity<?> confirmTransferPayment(@PathVariable Integer id) {
        String actor = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return ResponseEntity.ok(OrderResponse.from(orderPaymentService.confirmTransferPayment(id, actor)));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Transactional
    public ResponseEntity<?> payOrder(@PathVariable Integer id) {
        java.util.Optional<Order> orderOpt = orderRepository.findLockedById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (Integer.valueOf(3).equals(order.getStatus())) {
                return ResponseEntity.status(409).body("Không thể thanh toán đơn đã hủy!");
            }
            if (Boolean.TRUE.equals(order.getIsPaid())
                    || PaymentStatus.PAID.equals(order.getPaymentStatus())
                    || PaymentStatus.OVERPAID.equals(order.getPaymentStatus())) {
                return ResponseEntity.status(409).body("Đơn hàng đã được thanh toán trước đó");
            }
            if (OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())) {
                return ResponseEntity.status(409)
                        .body("Đơn chuyển khoản chỉ được xác nhận qua payment ledger/webhook!");
            }
            boolean firstPaymentConfirmation = !Boolean.TRUE.equals(order.getIsPaid());
            order.setIsPaid(true);
            java.math.BigDecimal paid = money(order.getTotalAmount()).setScale(0, java.math.RoundingMode.HALF_UP);
            order.setPaidAmount(paid);
            order.setRemainingAmount(java.math.BigDecimal.ZERO);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentConfirmedBy(org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName());
            order.setPaymentConfirmedAt(new Date());
            inventoryReservationService.consume(id);
            orderStateMachineService.transition(order, OrderStatus.COMPLETED);
            orderRepository.saveAndFlush(order);
            if (order.getTableId() != null) {
                tableLifecycleService.markCleaningAfterPayment(order.getTableId(), order.getId());
            }
            if (firstPaymentConfirmation) {
                awardOrderPoints(order);
            }
            activityLogService.log("MANUAL_PAYMENT_CONFIRM", "Order", String.valueOf(id),
                    "Thu ngân/quản lý xác nhận thanh toán thủ công");
            messagingTemplate.convertAndSend("/topic/waiter", "ORDER_PAID");
            return ResponseEntity.ok(OrderResponse.from(order));
        }
        return ResponseEntity.badRequest().body("Đơn hàng không tồn tại!");
    }

    @PutMapping("/{id}/confirm-manual")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Transactional
    public ResponseEntity<?> confirmManualOrder(@PathVariable Integer id) {
        return ResponseEntity.ok(orderPaymentService.confirmManualDispatch(id));
    }

    @PutMapping("/{id}/dispatch-to-kitchen")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAITER')")
    @Transactional
    public ResponseEntity<?> dispatchToKitchen(@PathVariable Integer id) {
        return ResponseEntity.ok(OrderResponse.from(orderPaymentService.confirmManualDispatch(id)));
    }

    @PostMapping("/{id}/payment-qr")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<?> createPaymentQr(@PathVariable Integer id) {
        return ResponseEntity.ok(orderPaymentService.createForExistingOrder(id));
    }

    @PostMapping("/{id}/payment-qr/{paymentCode}/regenerate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<?> regeneratePaymentQr(
            @PathVariable Integer id,
            @PathVariable String paymentCode,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(orderPaymentService.regenerate(id, paymentCode, idempotencyKey));
    }

    private void requireManualConfirmationRole() {
        boolean allowed = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_MANAGER")
                        || authority.getAuthority().equals("ROLE_CASHIER")
                        || authority.getAuthority().equals("ROLE_WAITER"));
        if (!allowed) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ phục vụ, thu ngân hoặc quản lý được chuyển đơn vào bếp");
        }
    }

    private void awardOrderPoints(Order order) {
        if (order.getAccount() == null) {
            return;
        }
        int points = money(order.getTotalAmount())
                .divideToIntegralValue(BigDecimal.valueOf(10_000))
                .intValue();
        if (points > 0) {
            pointsLedgerService.credit(
                    order.getAccount().getUsername(),
                    PointsEventType.ORDER_COMPLETED,
                    "ORDER_COMPLETED:" + order.getId(),
                    points,
                    "Thưởng điểm đơn đã thanh toán #" + order.getId());
        }
    }

    // 🌟 API MỚI: CHUYỂN BÀN (Cập nhật địa chỉ đơn hàng)
    private BigDecimal orderTotal(Order order) {
        if (order.getTotalAmount() != null && order.getTotalAmount().signum() > 0) {
            return money(order.getTotalAmount());
        }
        return order.getOrderDetails().stream()
                .map(detail -> money(detail.getPrice()).add(money(detail.getTaxAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    @PutMapping("/{id}/address")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAITER')")
    public ResponseEntity<?> updateOrderAddress(@PathVariable Integer id, @RequestParam String newAddress) {
        return orderRepository.findById(id).map(order -> {
            order.setAddress(newAddress);
            orderRepository.save(order);
            return ResponseEntity.ok("Cập nhật địa chỉ/bàn thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Integer id,
                                          @Valid @RequestBody poly.edu.quanlynhahang.dto.OrderCancelRequest body) {
        String actor = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderRefundService.cancelAndRequestRefund(id, actor));
    }

    @PutMapping("/{id}/cancel-with-refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    @Transactional
    public ResponseEntity<?> cancelOrderWithRefund(@PathVariable Integer id) {
        String actor = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderRefundService.cancelAndRequestRefund(id, actor));
    }

    private boolean isRevenueOrder(Order order) {
        if (order == null || Integer.valueOf(OrderStatus.CANCELLED.code()).equals(order.getStatus())
                || PaymentStatus.REFUNDED.equals(order.getPaymentStatus())) {
            return false;
        }
        return order.getStatus() == OrderStatus.COMPLETED.code()
                || order.getStatus() == OrderStatus.SERVED.code()
                || Boolean.TRUE.equals(order.getIsPaid())
                || PaymentStatus.PAID.equals(order.getPaymentStatus());
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/refund-complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<?> completeOrderRefund(@PathVariable Integer id,
                                                  @Valid @RequestBody RefundCompletionRequest request) {
        String actor = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderRefundService.completeRefund(
                id, request.providerReference(), request.note(), actor));
    }

    @PutMapping("/{id}/table")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAITER')")
    @Transactional
    public ResponseEntity<?> moveOrderToTable(@PathVariable Integer id, @RequestParam Integer newTableId) {
        Order snapshot = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (!OrderType.DINE_IN.equals(snapshot.getOrderType())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ đơn tại bàn mới được chuyển bàn");
        }
        if (Integer.valueOf(3).equals(snapshot.getStatus()) || Integer.valueOf(4).equals(snapshot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể chuyển bàn cho đơn đã đóng");
        }
        Integer oldTableId = snapshot.getTableId();
        if (newTableId.equals(oldTableId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã ở bàn này");
        }
        List<Integer> tableIds = oldTableId == null
                ? List.of(newTableId)
                : java.util.stream.Stream.of(oldTableId, newTableId).distinct().sorted().toList();
        List<RestaurantTable> lockedTables = tableRepository.findLockedByIdIn(tableIds);
        if (lockedTables.size() != tableIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn nguồn hoặc bàn đích");
        }
        Map<Integer, RestaurantTable> tablesById = lockedTables.stream()
                .collect(Collectors.toMap(RestaurantTable::getId, table -> table));
        RestaurantTable target = tablesById.get(newTableId);
        Order order = orderRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (!java.util.Objects.equals(oldTableId, order.getTableId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bàn của đơn vừa được thay đổi, vui lòng tải lại");
        }
        if (Boolean.FALSE.equals(target.getActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn đích đang ngừng hoạt động");
        }
        if ((target.getIsOccupied() != null && target.getIsOccupied() != 0)
                || orderRepository.existsActiveOrderForTableExcludingOrder(newTableId, order.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn đích đang được sử dụng");
        }
        order.setTableId(newTableId);
        order.setRestaurantTable(target);
        orderRepository.save(order);
        target.setIsOccupied(2);
        tableRepository.save(target);
        if (oldTableId != null && !oldTableId.equals(newTableId)) {
            tableSessionService.revokeActiveForTable(oldTableId);
            if (!orderRepository.existsActiveOrderForTableExcludingOrder(oldTableId, order.getId())) {
                tableLifecycleService.release(tablesById.get(oldTableId).getId());
            }
        }
        tableSessionService.revokeActiveForTable(newTableId);
        activityLogService.log("MOVE_TABLE", "Order", String.valueOf(id),
                "Chuyển đơn từ bàn " + oldTableId + " sang bàn " + newTableId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    // 🌟 API MỚI: TỰ ĐỘNG KÍCH HOẠT ĐƠN ĐẶT BÀN HẸN GIờ
    // Frontend gọi mỗi 30 giây. Nếu có đơn status=5 và hiện tại ≥ giờ hẹn - 15 phút → chuyển status=1
    @PutMapping("/activate-scheduled")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional
    public ResponseEntity<?> activateScheduledOrders() {
        List<Order> scheduledOrders = orderRepository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(5, LocalDateTime.now().plusMinutes(15));
        int activated = 0;
        for (Order order : scheduledOrders) {
            orderStateMachineService.transition(order, OrderStatus.IN_PREPARATION);
            orderRepository.save(order);
            activated++;
        }

        if (activated > 0) {
            messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("activated", activated);
        result.put("message", activated > 0 ? "Đã kích hoạt " + activated + " đơn hẹn giờ!" : "Chưa có đơn nào tới hạn.");
        return ResponseEntity.ok(result);
    }
}
