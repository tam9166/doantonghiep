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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.repository.OrderRepository;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/orders")
// ✅ FIX: Dùng hasAnyAuthority với ROLE_ prefix đầy đủ
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderRepository.findAll().stream()
                .sorted((o1, o2) -> o2.getId().compareTo(o1.getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }

    // THỐNG KÊ (Khóa lại chỉ cho Quản lý xem)
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getRevenueAnalytics() {
        List<Order> allOrders = orderRepository.findAll();
        List<Order> completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 4)
                .collect(Collectors.toList());
        double totalRevenue = 0;
        int totalItemsSold = 0;
        for (Order order : completedOrders) {
            if (order.getOrderDetails() != null) {
                totalRevenue += order.getOrderDetails().stream().mapToDouble(d -> d.getPrice()).sum();
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
    public ResponseEntity<?> getDashboardStats() {
        List<Order> completedOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 4)
                .collect(Collectors.toList());

        // 1. Doanh thu 7 ngày qua
        Map<String, Double> revenueByDate = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        for (Order o : completedOrders) {
            if (o.getCreateDate() != null && o.getOrderDetails() != null) {
                String dateStr = sdf.format(o.getCreateDate());
                double rev = o.getOrderDetails().stream().mapToDouble(d -> d.getPrice()).sum();
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

    @Autowired
    private poly.edu.quanlynhahang.repository.AccountRepository accountRepository;

    // 🌟 API MỚI: XỬ LÝ NÚT BẤM "XONG MÓN" HOẶC "ĐÃ BƯNG RA BÀN"
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Integer id, @RequestParam Integer status) {
        return orderRepository.findById(id).map(order -> {
            if (status == 4 && order.getStatus() != 4 && order.getAccount() != null) {
                poly.edu.quanlynhahang.entity.Account acc = order.getAccount();
                double total = order.getOrderDetails().stream().mapToDouble(d -> d.getPrice() * d.getQuantity()).sum();
                acc.setPoints((acc.getPoints() != null ? acc.getPoints() : 0) + (int)(total / 10000));
                
                if (acc.getPoints() >= 2000) acc.setMembershipTier("Kim Cương");
                else if (acc.getPoints() >= 1000) acc.setMembershipTier("Vàng");
                else if (acc.getPoints() >= 500) acc.setMembershipTier("Bạc");
                
                accountRepository.save(acc);
            }
            order.setStatus(status);
            orderRepository.save(order);
            
            if (status == 1 || status == 6) {
                messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
            } else if (status == 2) {
                messagingTemplate.convertAndSend("/topic/waiter", "ORDER_READY");
            }
            
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!"));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_WAITER')")
    public ResponseEntity<?> payOrder(@PathVariable Integer id) {
        java.util.Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setIsPaid(true);
            order.setStatus(4); 
            orderRepository.save(order);
            messagingTemplate.convertAndSend("/topic/waiter", "ORDER_PAID");
            return ResponseEntity.ok(order);
        }
        return ResponseEntity.badRequest().body("Đơn hàng không tồn tại!");
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
    public ResponseEntity<?> cancelOrder(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(3);
            orderRepository.save(order);
            return ResponseEntity.ok("Hủy đơn hàng thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!"));
    }

    // 🌟 API MỚI: TỰ ĐỘNG KÍCH HOẠT ĐƠN ĐẶT BÀN HẸN GIờ
    // Frontend gọi mỗi 30 giây. Nếu có đơn status=5 và hiện tại ≥ giờ hẹn - 15 phút → chuyển status=1
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