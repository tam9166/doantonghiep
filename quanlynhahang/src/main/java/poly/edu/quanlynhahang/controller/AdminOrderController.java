package poly.edu.quanlynhahang.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.dto.OrderResponse;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.PointsLedgerService;
import poly.edu.quanlynhahang.service.OrderPaymentService;
@RestController
@RequestMapping("/api/admin/orders")
// ✅ FIX: Dùng hasAnyAuthority với ROLE_ prefix đầy đủ
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
public class AdminOrderController {

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

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderRepository.findAllWithDetails().stream()
                .sorted((o1, o2) -> o2.getId().compareTo(o1.getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(orders.stream().map(OrderResponse::from).toList());
    }

    // THỐNG KÊ (Khóa lại chỉ cho Quản lý xem)
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRevenueAnalytics() {
        List<Order> allOrders = orderRepository.findAllWithDetails();
        List<Order> completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 4)
                .collect(Collectors.toList());
        double totalRevenue = 0;
        int totalItemsSold = 0;
        for (Order order : completedOrders) {
            if (order.getOrderDetails() != null) {
                totalRevenue += order.getTotalAmount() != null && order.getTotalAmount() > 0 ? order.getTotalAmount() : order.getOrderDetails().stream().mapToDouble(d -> (d.getPrice() != null ? d.getPrice() : 0) + (d.getTaxAmount() != null ? d.getTaxAmount() : 0)).sum();
                totalItemsSold += order.getOrderDetails().stream().mapToInt(d -> d.getQuantity()).sum();
            }
        }
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRevenue", totalRevenue);
        statistics.put("completedOrdersCount", completedOrders.size());
        statistics.put("totalItemsSold", totalItemsSold);
        statistics.put("pendingOrdersCount", allOrders.stream().filter(o -> o.getStatus() == 0).count());
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDashboardStats() {
        List<Order> completedOrders = orderRepository.findAllWithDetails().stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 4)
                .collect(Collectors.toList());

        // 1. Doanh thu 7 ngày qua
        Map<String, Double> revenueByDate = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        for (Order o : completedOrders) {
            if (o.getCreateDate() != null && o.getOrderDetails() != null) {
                String dateStr = sdf.format(o.getCreateDate());
                double rev = o.getTotalAmount() != null && o.getTotalAmount() > 0 ? o.getTotalAmount() : o.getOrderDetails().stream().mapToDouble(d -> (d.getPrice() != null ? d.getPrice() : 0) + (d.getTaxAmount() != null ? d.getTaxAmount() : 0)).sum();
                revenueByDate.put(dateStr, revenueByDate.getOrDefault(dateStr, 0.0) + rev);
            }
        }

        // 2. Top 5 sản phẩm bán chạy
        Map<String, Integer> productSales = new HashMap<>();
        for (Order o : completedOrders) {
            if (o.getOrderDetails() != null) {
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

        return ResponseEntity.ok(stats);
    }

    // 🌟 API MỚI: XỬ LÝ NÚT BẤM "XONG MÓN" HOẶC "ĐÃ BƯNG RA BÀN"
    @PutMapping("/{id}/status")
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
            if (status == 4 && order.getStatus() != 4 && Boolean.TRUE.equals(order.getIsPaid())) {
                awardOrderPoints(order);
            }
            order.setStatus(status);
            orderRepository.save(order);
            
            String[] statusLabels = {"Đang chờ", "Đang làm món", "Đã gửi bếp", "Đã hủy", "Hoàn thành", "Đặt trước", "Gửi lại bếp"};
            String statusText = status >= 0 && status < statusLabels.length ? statusLabels[status] : "status=" + status;
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
            if (OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())) {
                return ResponseEntity.status(409)
                        .body("Đơn chuyển khoản chỉ được xác nhận qua payment ledger/webhook!");
            }
            boolean firstPaymentConfirmation = !Boolean.TRUE.equals(order.getIsPaid());
            order.setIsPaid(true);
            java.math.BigDecimal paid = java.math.BigDecimal.valueOf(
                    order.getTotalAmount() == null ? 0.0 : order.getTotalAmount()).setScale(0, java.math.RoundingMode.HALF_UP);
            order.setPaidAmount(paid);
            order.setRemainingAmount(java.math.BigDecimal.ZERO);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentConfirmedBy(org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName());
            order.setPaymentConfirmedAt(new Date());
            order.setStatus(4); 
            orderRepository.save(order);
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
    public ResponseEntity<?> confirmManualOrder(@PathVariable Integer id) {
        return ResponseEntity.ok(orderPaymentService.confirmManualDispatch(id));
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
                        || authority.getAuthority().equals("ROLE_CASHIER"));
        if (!allowed) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chỉ thu ngân hoặc quản lý được xác nhận đơn thủ công");
        }
    }

    private void awardOrderPoints(Order order) {
        if (order.getAccount() == null) {
            return;
        }
        double total = order.getTotalAmount() == null ? 0.0 : order.getTotalAmount();
        int points = (int) Math.floor(total / 10_000.0);
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
    @PutMapping("/{id}/address")
    public ResponseEntity<?> updateOrderAddress(@PathVariable Integer id, @RequestParam String newAddress) {
        return orderRepository.findById(id).map(order -> {
            order.setAddress(newAddress);
            orderRepository.save(order);
            return ResponseEntity.ok("Cập nhật địa chỉ/bàn thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!"));
    }

    @PutMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return orderRepository.findById(id).map(order -> {
            reverseOrderPointsIfAwarded(order, "ORDER_CANCELLED");
            order.setStatus(3);
            orderRepository.save(order);
            return ResponseEntity.ok("Hủy đơn hàng thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!"));
    }

    @PutMapping("/{id}/cancel-with-refund")
    @Transactional
    public ResponseEntity<?> cancelOrderWithRefund(@PathVariable Integer id) {
        return orderRepository.findById(id).map(order -> {
            reverseOrderPointsIfAwarded(order, "ORDER_REFUNDED");
            order.setStatus(3);
            Double refundAmount = (order.getDeposit() != null ? order.getDeposit() : 0.0) / 2.0;
            orderRepository.save(order);
            
            // Giải phóng bàn (nếu có)
            if (order.getAddress() != null) {
                // Address: "MÃ ĐƠN: #1234 | Bàn: Tầng 2 - Sảnh..."
                String address = order.getAddress();
                if (address.contains("Bàn: ")) {
                    String[] parts = address.split("\\|");
                    for (String part : parts) {
                        if (part.trim().startsWith("Bàn: ")) {
                            String tableName = part.replace("Bàn: ", "").trim();
                            // Loại bỏ các chữ dư thừa có thể sinh ra như lúc, ngày
                            if (tableName.contains(" |")) {
                                tableName = tableName.substring(0, tableName.indexOf(" |"));
                            }
                            // Giải phóng bàn
                            List<RestaurantTable> tables = tableRepository.findAll();
                            for (RestaurantTable t : tables) {
                                if (t.getName().equalsIgnoreCase(tableName)) {
                                    t.setIsOccupied(0);
                                    t.setReservedTime(null);
                                    tableRepository.save(t);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            return ResponseEntity.ok(java.util.Map.of(
                "message", "Hủy bàn thành công. Cần hoàn lại: " + refundAmount + "đ",
                "refundAmount", refundAmount
            ));
        }).orElse(ResponseEntity.badRequest().body(java.util.Map.of("message", "Không tìm thấy đơn hàng!")));
    }

    // 🌟 API MỚI: TỰ ĐỘNG KÍCH HOẠT ĐƠN ĐẶT BÀN HẸN GIờ
    // Frontend gọi mỗi 30 giây. Nếu có đơn status=5 và hiện tại ≥ giờ hẹn - 15 phút → chuyển status=1
    private void reverseOrderPointsIfAwarded(Order order, String eventPrefix) {
        if (order.getAccount() == null || order.getId() == null) {
            return;
        }
        pointsLedgerService.reverseIfPresent(
                order.getAccount().getUsername(),
                "ORDER_COMPLETED:" + order.getId(),
                eventPrefix + ":" + order.getId(),
                "Thu hoi diem do huy/hoan tien don #" + order.getId());
    }

    @PutMapping("/activate-scheduled")
    public ResponseEntity<?> activateScheduledOrders() {
        List<Order> scheduledOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 5)
                .collect(Collectors.toList());

        int activated = 0;
        Date now = new Date();

        for (Order order : scheduledOrders) {
            if (order.getAddress() == null) continue;

            // Parse giờ hẹn từ address: "Lúc: HH:mm ngày yyyy-MM-dd"
            Pattern pattern = Pattern.compile("Lúc:\\s*(\\d{2}:\\d{2})\\s*ngày\\s*(\\d{4}-\\d{2}-\\d{2})");
            Matcher matcher = pattern.matcher(order.getAddress());

            if (matcher.find()) {
                try {
                    String timeStr = matcher.group(1);
                    String dateStr = matcher.group(2);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    Date arrivalTime = sdf.parse(dateStr + " " + timeStr);

                    // Nếu hiện tại ≥ giờ hẹn - 15 phút → kích hoạt
                    long diffMinutes = (arrivalTime.getTime() - now.getTime()) / (1000 * 60);
                    if (diffMinutes <= 15) {
                        order.setStatus(1); // Chuyển xuống bếp
                        orderRepository.save(order);
                        activated++;
                    }
                } catch (Exception e) {
                    // Skip lỗi parse
                }
            }
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
