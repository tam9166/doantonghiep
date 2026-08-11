package poly.edu.quanlynhahang.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
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

    @GetMapping("/history")
    public ResponseEntity<?> getMyOrders() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderRepository.findByAccountUsername(currentUsername).stream()
                .map(OrderResponse::from).toList());
    }

    @GetMapping("/open-by-table")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getOpenDineInOrder(@org.springframework.web.bind.annotation.RequestParam String tableName) {
        String normalizedTableName = tableName.trim();
        Integer tableId = tableRepository.findByName(normalizedTableName)
                .map(RestaurantTable::getId)
                .orElse(null);
        return orderRepository.findOpenDineInOrdersWithDetails(tableId, normalizedTableName).stream()
                .findFirst()
                .map(order -> ResponseEntity.ok(OrderResponse.from(order)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/guest-booking")
    public ResponseEntity<?> guestBooking(@Valid @RequestBody GuestBookingRequest payload) {
        String name = payload.customerName().trim();
        String phone = payload.phone().trim();
        String tableName = payload.tableName().trim();
        String time = payload.scheduledTime().trim();
        
        String uniqueOrderCode = generateUnique4DigitCode();
        Order order = new Order();
        order.setAccount(null); // Guest
        order.setAddress("Bàn " + tableName + " - Khách: " + name + " - SĐT: " + phone + " - Hẹn lúc: " + time);
        order.setCreateDate(new java.util.Date());
        order.setStatus(5); // 5 = Đặt bàn hẹn trước
        orderRepository.save(order);
        
        return ResponseEntity.ok(java.util.Map.of("message", "Đặt bàn thành công!", "orderCode", uniqueOrderCode));
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody OrderRequest orderRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderCheckoutService.checkout(orderRequest, username));
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
        String fromTable = payload.fromTable().trim();
        String toTable = payload.toTable().trim();
        if (fromTable.equalsIgnoreCase(toTable)) {
            return ResponseEntity.badRequest().body("Source and destination tables must differ.");
        }

        Optional<Order> anySourceOrder = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && o.getStatus() != 3)
            .findFirst();

        Optional<Order> anyTargetOrder = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && o.getStatus() != 3)
            .findFirst();

        if (anySourceOrder.isPresent() && anyTargetOrder.isPresent()) {
            boolean sourcePaid = Boolean.TRUE.equals(anySourceOrder.get().getIsPaid());
            boolean targetPaid = Boolean.TRUE.equals(anyTargetOrder.get().getIsPaid());
            if (sourcePaid != targetPaid) {
                return ResponseEntity.status(409).body("Không thể gộp bàn đã thanh toán với bàn chưa thanh toán!");
            }
        }
        
        Optional<Order> sourceOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
            
        Optional<Order> targetOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
            
        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn nguồn không có hóa đơn nào đang mở!");
        }
        if (targetOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn đích không có hóa đơn nào đang mở! Vui lòng order món cho bàn đích trước.");
        }
        
        Order sourceOrder = sourceOrderOpt.get();
        Order targetOrder = targetOrderOpt.get();
        
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
        orderRepository.save(targetOrder);
        
        // Hủy hóa đơn cũ
        sourceOrder.setStatus(3);
        orderRepository.save(sourceOrder);
        
        // Đánh dấu bàn cũ là Đã Ghép thay vì Trống
        tableRepository.findAll().stream()
            .filter(t -> t.getName().equals(fromTable))
            .findFirst()
            .ifPresent(t -> {
                t.setIsOccupied(5);
                t.setReservedTime("[GHÉP VỚI: " + toTable + "]");
                tableRepository.save(t);
            });
            
        messagingTemplate.convertAndSend("/topic/orders", "TABLE_MERGED");
        
        return ResponseEntity.ok(java.util.Map.of("message", "Gộp bàn thành công!"));
    }

    @PostMapping("/split-table")
    @Transactional
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> splitTable(@Valid @RequestBody SplitTableRequest payload) {
        String fromTable = payload.fromTable().trim();
        String toTable = payload.toTable().trim();
        if (fromTable.equalsIgnoreCase(toTable)) {
            return ResponseEntity.badRequest().body("Source and destination tables must differ.");
        }
        List<Integer> detailIds = payload.detailIds();

        Optional<Order> sourceOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(fromTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();

        if (sourceOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Bàn nguồn không có hóa đơn nào đang mở!");
        }

        Order sourceOrder = sourceOrderOpt.get();
        
        // Cố gắng tìm Order của bàn đích
        Optional<Order> targetOrderOpt = orderRepository.findAll().stream()
            .filter(o -> o.getAddress() != null && o.getAddress().contains(toTable) && (o.getIsPaid() == null || !o.getIsPaid()) && o.getStatus() != 3)
            .findFirst();
        
        Order targetOrder;
        if (targetOrderOpt.isPresent()) {
            targetOrder = targetOrderOpt.get();
        } else {
            // Tạo Order mới cho bàn đích
            String uniqueOrderCode = generateUnique4DigitCode();
            targetOrder = new Order();
            targetOrder.setAccount(sourceOrder.getAccount()); // copy account
            targetOrder.setAddress("MÃ ĐƠN: #" + uniqueOrderCode + " | Bàn: " + toTable + " | [TẠI QUÁN]");
            targetOrder.setCreateDate(new Date());
            targetOrder.setStatus(sourceOrder.getStatus()); // copy status
            targetOrder = orderRepository.save(targetOrder);
            
            // Cập nhật trạng thái bàn đích
            final String fUniqueOrderCode = uniqueOrderCode;
            tableRepository.findAll().stream()
                .filter(t -> t.getName().equals(toTable))
                .findFirst()
                .ifPresent(t -> {
                    t.setIsOccupied(2); // Có khách
                    t.setReservedTime("Đơn: #" + fUniqueOrderCode);
                    tableRepository.save(t);
                });
        }
        
        final Order finalTargetOrder = targetOrder;
        BigDecimal moveSub = BigDecimal.ZERO;
        BigDecimal moveTax = BigDecimal.ZERO;
        // Di chuyển các order detail
        for (Integer detailId : detailIds) {
            Optional<OrderDetail> detailOpt = orderDetailRepository.findById(detailId);
            if (detailOpt.isPresent()) {
                OrderDetail detail = detailOpt.get();
                if (detail.getOrder().getId().equals(sourceOrder.getId())) {
                    detail.setOrder(finalTargetOrder);
                    orderDetailRepository.save(detail);
                    moveSub = moveSub.add(money(detail.getPrice()));
                    moveTax = moveTax.add(money(detail.getTaxAmount()));
                }
            }
        }
        
        BigDecimal sourceSubTotal = money(sourceOrder.getSubTotal()).subtract(moveSub).max(BigDecimal.ZERO);
        BigDecimal sourceTaxAmount = money(sourceOrder.getTaxAmount()).subtract(moveTax).max(BigDecimal.ZERO);
        sourceOrder.setSubTotal(sourceSubTotal);
        sourceOrder.setTaxAmount(sourceTaxAmount);
        sourceOrder.setTotalAmount(sourceSubTotal.add(sourceTaxAmount));
        orderRepository.save(sourceOrder);
        
        BigDecimal finalTargetSubTotal = money(finalTargetOrder.getSubTotal()).add(moveSub);
        BigDecimal finalTargetTaxAmount = money(finalTargetOrder.getTaxAmount()).add(moveTax);
        finalTargetOrder.setSubTotal(finalTargetSubTotal);
        finalTargetOrder.setTaxAmount(finalTargetTaxAmount);
        finalTargetOrder.setTotalAmount(finalTargetSubTotal.add(finalTargetTaxAmount));
        orderRepository.save(finalTargetOrder);

        // Nếu bàn nguồn không còn OrderDetail nào, thì Hủy order đó và giải phóng bàn
        long remainingItems = orderDetailRepository.findAll().stream()
            .filter(d -> d.getOrder().getId().equals(sourceOrder.getId()))
            .count();
            
        if (remainingItems == 0) {
            sourceOrder.setStatus(3); // Hủy
            orderRepository.save(sourceOrder);
            tableRepository.findAll().stream()
                .filter(t -> t.getName().equals(fromTable))
                .findFirst()
                .ifPresent(t -> {
                    t.setIsOccupied(0);
                    t.setReservedTime(null);
                    tableRepository.save(t);
                });
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
        return orderDetailRepository.findById(detailId).map(detail -> {
            if (status == 1 && detail.getStartedAt() == null) {
                return ResponseEntity.status(409).body("Món phải được bếp bắt đầu chế biến trước khi hoàn thành.");
            }
            if (status == 2 && (!Integer.valueOf(1).equals(detail.getStatus()) || detail.getCompletedAt() == null)) {
                return ResponseEntity.status(409).body("Phục vụ chỉ được bưng món đã được bếp hoàn thành.");
            }
            detail.setStatus(status);
            orderDetailRepository.save(detail);

            Order order = detail.getOrder();
            if (order != null) {
                boolean allDone = true;
                boolean anyReady = false;
                if (order.getOrderDetails() != null) {
                    for (OrderDetail d : order.getOrderDetails()) {
                        if (d.getStatus() == null || d.getStatus() == 0) {
                            allDone = false;
                        }
                        if (d.getStatus() != null && d.getStatus() == 1) {
                            anyReady = true;
                        }
                    }
                }
                
                if (allDone && (order.getStatus() == 1 || order.getStatus() == 6)) {
                    order.setStatus(2); // Cả bàn đã xong, chờ bưng
                    orderRepository.save(order);
                } else if (anyReady && order.getStatus() == 1) {
                    order.setStatus(6); // Đang nấu (có món xong trước)
                    orderRepository.save(order);
                }

                messagingTemplate.convertAndSend("/topic/waiter", "DISH_STATUS_CHANGED");
                messagingTemplate.convertAndSend("/topic/kitchen", "DISH_STATUS_CHANGED");
            }
            return ResponseEntity.ok("Cập nhật món thành công!");
        }).orElse(ResponseEntity.badRequest().body("Lỗi không tìm thấy món!"));
    }

    private String generateUnique4DigitCode() {
        Random random = new Random();
        String code;
        boolean isDuplicate;
        do {
            int number = random.nextInt(9000) + 1000;
            code = String.valueOf(number);
            final String checkCode = "#" + code;
            isDuplicate = orderRepository.findAll().stream()
                .anyMatch(o -> o.getAddress() != null && o.getAddress().contains(checkCode));
        } while (isDuplicate);
        return code;
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
                                                @Valid @RequestBody KitchenDishCancelRequest request) {
        return ResponseEntity.ok(poly.edu.quanlynhahang.dto.OrderDetailResponse
                .from(kitchenOrderDetailService.cancel(detailId, request.reason())));
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
