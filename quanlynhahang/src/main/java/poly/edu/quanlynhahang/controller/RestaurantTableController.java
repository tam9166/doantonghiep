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
        // Chỉ lấy trạng thái thực tế từ DB, không tính toán phức tạp nữa
        return ResponseEntity.ok(tableRepository.findAll());
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
            if (status == 0) {
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
}