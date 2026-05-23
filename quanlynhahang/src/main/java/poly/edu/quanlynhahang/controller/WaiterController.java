package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.repository.OrderRepository;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/waiter")
@PreAuthorize("hasAnyAuthority('ROLE_WAITER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
public class WaiterController {

    @Autowired
    private OrderRepository orderRepository;

    // 1. Lấy danh sách món Bếp đã nấu xong (Status = 2)
    @GetMapping("/ready-orders")
    public ResponseEntity<?> getReadyOrders() {
        return ResponseEntity.ok(orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == 2)
                .toList());
    }

    // 2. Xác nhận đã bưng món ra bàn (Chuyển Status sang 3 - Đang ăn)
    @PutMapping("/orders/{id}/serve")
    public ResponseEntity<?> confirmServed(@PathVariable Long id) {
        return orderRepository.findById(id).map(order -> {
            order.setStatus(3);
            orderRepository.save(order);
            return ResponseEntity.ok("Đã bưng ra bàn, khách bắt đầu dùng bữa!");
        }).orElse(ResponseEntity.notFound().build());
    }
}