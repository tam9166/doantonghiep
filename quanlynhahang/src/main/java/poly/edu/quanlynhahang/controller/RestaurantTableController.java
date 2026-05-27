package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/tables")
public class RestaurantTableController {

    @Autowired private RestaurantTableRepository tableRepository;
    @Autowired private OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<?> getAllTables() {
        return ResponseEntity.ok(tableRepository.findAll());
    }

    @GetMapping("/seed")
    public ResponseEntity<?> seedTables() {
        if (tableRepository.count() > 20) {
            return ResponseEntity.ok("Đã có đủ bàn, không cần thêm nữa!");
        }

        // Tầng 2: Sảnh tiệc (15 bàn, sức chứa 10)
        for (int i = 1; i <= 15; i++) {
            tableRepository.save(new RestaurantTable(null, "Bàn T2-" + String.format("%02d", i), "Tầng 2 (Sảnh Tiệc)", 0, false, null, 10, null));
        }

        // Tầng 3, 4, 5: Phòng VIP (5 bàn mỗi tầng)
        for (int i = 1; i <= 5; i++) {
            tableRepository.save(new RestaurantTable(null, "VIP T3-" + String.format("%02d", i), "Tầng 3 (Phòng VIP)", 0, false, null, 6, null));
            tableRepository.save(new RestaurantTable(null, "VIP T4-" + String.format("%02d", i), "Tầng 4 (Phòng VIP)", 0, false, null, 6, null));
            tableRepository.save(new RestaurantTable(null, "VIP T5-" + String.format("%02d", i), "Tầng 5 (Phòng VIP)", 0, false, null, 8, null));
        }

        // Tầng 6: Sân Thượng
        for (int i = 1; i <= 4; i++) {
            tableRepository.save(new RestaurantTable(null, "Rooftop T6-" + String.format("%02d", i), "Tầng 6 (Sân Thượng)", 0, false, null, 4, "View Phố"));
            tableRepository.save(new RestaurantTable(null, "Rooftop T6-" + String.format("%02d", i + 4), "Tầng 6 (Sân Thượng)", 0, false, null, 4, "View Sông"));
            tableRepository.save(new RestaurantTable(null, "Rooftop T6-" + String.format("%02d", i + 8), "Tầng 6 (Sân Thượng)", 0, false, null, 6, "View Sân Vườn"));
        }

        return ResponseEntity.ok("Đã tạo tự động các bàn thành công!");
    }
    
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(@RequestParam String date, @RequestParam String time) {
        List<RestaurantTable> allTables = tableRepository.findAll();
        List<Order> bookedOrders = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() != null && o.getStatus() < 4)
            .filter(o -> o.getAddress() != null && o.getAddress().contains(date))
            .collect(Collectors.toList());

        allTables.forEach(table -> {
            boolean isOverlapping = bookedOrders.stream()
                .anyMatch(order -> order.getAddress().contains(table.getName()));
            if (isOverlapping && table.getIsOccupied() == 0) {
                table.setIsOccupied(1); 
                table.setReservedTime("Khách đặt: " + time);
            }
        });
        return ResponseEntity.ok(allTables);
    }

    @PostMapping
    public RestaurantTable addTable(@RequestBody RestaurantTable table) {
        table.setIsOccupied(0); 
        return tableRepository.save(table);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestParam Integer status, @RequestParam(required = false) String time) {
        return tableRepository.findById(id).map(table -> {
            table.setIsOccupied(status);
            table.setReservedTime(status == 0 ? null : time);
            tableRepository.save(table);

            // 🌟 TÍNH NĂNG DOANH NGHIỆP: Dọn bàn tự động chốt các đơn hàng đang treo
            if (status == 0 || status == 3) {
                List<Order> activeOrders = orderRepository.findAll().stream()
                    .filter(o -> o.getStatus() != null && o.getStatus() < 4)
                    .filter(o -> o.getAddress() != null && o.getAddress().contains(table.getName()))
                    .collect(Collectors.toList());
                for (Order o : activeOrders) {
                    o.setStatus(4); // 4 = Đã thanh toán xong
                    orderRepository.save(o);
                }
            }
            return ResponseEntity.ok("Cập nhật thành công!");
        }).orElse(ResponseEntity.badRequest().body("Lỗi!"));
    }

    @PutMapping("/{id}/link/{targetId}")
    public ResponseEntity<?> linkTable(@PathVariable Integer id, @PathVariable Integer targetId) {
        java.util.Optional<RestaurantTable> sourceOpt = tableRepository.findById(id);
        java.util.Optional<RestaurantTable> targetOpt = tableRepository.findById(targetId);
        
        if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy bàn!");
        }
        
        RestaurantTable source = sourceOpt.get();
        RestaurantTable target = targetOpt.get();
        
        source.setIsOccupied(5); // 5 = Trạng thái Đã Ghép
        source.setReservedTime("[GHÉP VỚI: " + target.getName() + "]");
        tableRepository.save(source);
        
        return ResponseEntity.ok("Ghép bàn thành công!");
    }

    @PutMapping("/{id}/unlink")
    public ResponseEntity<?> unlinkTable(@PathVariable Integer id) {
        return tableRepository.findById(id).map(table -> {
            table.setIsOccupied(0);
            table.setReservedTime(null);
            tableRepository.save(table);
            return ResponseEntity.ok("Tách bàn thành công!");
        }).orElse(ResponseEntity.badRequest().body("Lỗi không tìm thấy bàn!"));
    }
}