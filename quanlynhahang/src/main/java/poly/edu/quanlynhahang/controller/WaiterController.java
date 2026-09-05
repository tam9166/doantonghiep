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
import java.util.List;

import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.service.OrderStateMachineService;
import poly.edu.quanlynhahang.dto.OrderResponse;
import poly.edu.quanlynhahang.service.OrderServiceDateGuardService;
@RestController
@RequestMapping("/api/waiter")
@PreAuthorize("hasAnyAuthority('ROLE_WAITER', 'ROLE_ADMIN', 'ROLE_MANAGER')")
public class WaiterController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStateMachineService orderStateMachineService;

    @Autowired
    private OrderServiceDateGuardService serviceDateGuard;

    // 1. Lấy mọi đơn còn món trong vòng đời bếp/phục vụ. Trạng thái món là
    // nguồn sự thật; trạng thái đơn cha không biểu diễn đủ món gọi thêm.
    @GetMapping("/ready-orders")
    public ResponseEntity<?> getReadyOrders() {
        return ResponseEntity.ok(orderRepository.findWaiterOperationalOrdersWithDetails(
                        OrderStatus.CANCELLED.code(), poly.edu.quanlynhahang.entity.PaymentStatus.REFUNDED,
                        List.of(0, 1)).stream()
                .filter(order -> serviceDateGuard == null || serviceDateGuard.isPreparationReached(order))
                .map(order -> serviceDateGuard == null ? OrderResponse.from(order)
                        : OrderResponse.from(order, serviceDateGuard.resolveTiming(order))).toList());
    }

    // 2. Xác nhận đã bưng món ra bàn (Chuyển Status sang 3 - Đang ăn)
    @PutMapping("/orders/{id}/serve")
    @Transactional
    public ResponseEntity<?> confirmServed(@PathVariable Integer id) {
        return orderRepository.findLockedById(id).map(order -> {
            if (serviceDateGuard != null) serviceDateGuard.assertPreparationReached(order);
            List<OrderDetail> readyDetails = order.getOrderDetails() == null ? List.of()
                    : order.getOrderDetails().stream()
                            .filter(detail -> Integer.valueOf(1).equals(detail.getStatus()))
                            .toList();
            if (readyDetails.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Món này đã hoàn thành hoặc đã được xử lý.");
            }
            boolean allActiveWillBeServed = order.getOrderDetails().stream()
                    .filter(detail -> !Integer.valueOf(3).equals(detail.getStatus()))
                    .allMatch(detail -> Integer.valueOf(2).equals(detail.getStatus())
                            || readyDetails.contains(detail));
            OrderStatus current = orderStateMachineService.current(order);
            if (allActiveWillBeServed && current != OrderStatus.COMPLETED && current != OrderStatus.READY) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Trạng thái đơn không cho phép hoàn tất phục vụ món này");
            }
            readyDetails.forEach(detail -> detail.setStatus(2));
            if (allActiveWillBeServed && current != OrderStatus.COMPLETED) {
                orderStateMachineService.transition(order, OrderStatus.SERVED);
            }
            orderRepository.save(order);
            return ResponseEntity.ok("Đã bưng ra bàn, khách bắt đầu dùng bữa!");
        }).orElse(ResponseEntity.notFound().build());
    }
}
