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

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.repository.OrderRepository;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/orders")
// ✅ FIX: Dùng hasAnyAuthority với ROLE_ prefix đầy đủ
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER')")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

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

    // 🌟 API MỚI: XỬ LÝ NÚT BẤM "XONG MÓN" HOẶC "ĐÃ BƯNG RA BÀN"
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(status);
            orderRepository.save(order);
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        }).orElse(ResponseEntity.badRequest().body("Không tìm thấy đơn hàng!"));
    }

    // 🌟 API MỚI: CHUYỂN BÀN (Cập nhật địa chỉ đơn hàng)
    @PutMapping("/{id}/address")
    public ResponseEntity<?> updateOrderAddress(@PathVariable Long id, @RequestParam String newAddress) {
        return orderRepository.findById(id).map(order -> {
            order.setAddress(newAddress);
            orderRepository.save(order);
            return ResponseEntity.ok("Cập nhật địa chỉ/bàn thành công!");
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

        Map<String, Object> result = new HashMap<>();
        result.put("activated", activated);
        result.put("message", activated > 0 ? "Đã kích hoạt " + activated + " đơn hẹn giờ!" : "Chưa có đơn nào tới hạn.");
        return ResponseEntity.ok(result);
    }
}