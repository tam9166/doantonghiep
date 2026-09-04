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
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.service.OrderStateMachineService;
import poly.edu.quanlynhahang.dto.OrderResponse;
@RestController
@RequestMapping("/api/waiter")
@PreAuthorize("hasAnyAuthority('ROLE_WAITER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
public class WaiterController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStateMachineService orderStateMachineService;

    // 1. Lấy đơn có món đã hoàn thành ở cấp detail (kể cả đơn cha đang PARTIALLY_READY)
    @GetMapping("/ready-orders")
    public ResponseEntity<?> getReadyOrders() {
        return ResponseEntity.ok(orderRepository.findWaiterReadyOrdersWithDetails(
                        OrderStatus.READY.code(), 1).stream()
                .map(OrderResponse::from).toList());
    }

    // 2. Xác nhận đã bưng món ra bàn (Chuyển Status sang 3 - Đang ăn)
    @PutMapping("/orders/{id}/serve")
    @Transactional
    public ResponseEntity<?> confirmServed(@PathVariable Integer id) {
        return orderRepository.findLockedById(id).map(order -> {
            if (order.getOrderDetails() != null) {
                order.getOrderDetails().stream()
                        .filter(detail -> Integer.valueOf(1).equals(detail.getStatus()))
                        .forEach(detail -> detail.setStatus(2));
            }
            orderStateMachineService.transition(order, OrderStatus.SERVED);
            orderRepository.save(order);
            return ResponseEntity.ok("Đã bưng ra bàn, khách bắt đầu dùng bữa!");
        }).orElse(ResponseEntity.notFound().build());
    }
}
