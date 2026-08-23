package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;

@Service
public class TableReleaseGuardService {
    private static final EnumSet<PaymentStatus> BLOCKING_PAYMENT_STATUSES = EnumSet.of(
            PaymentStatus.PENDING, PaymentStatus.PARTIALLY_PAID,
            PaymentStatus.OVERPAID, PaymentStatus.REFUND_PENDING,
            PaymentStatus.PARTIALLY_REFUNDED);

    private final OrderRepository orderRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final OrderStateMachineService orderStateMachineService;

    public TableReleaseGuardService(OrderRepository orderRepository,
                                    PaymentIntentRepository paymentIntentRepository,
                                    InventoryReservationRepository inventoryReservationRepository,
                                    OrderStateMachineService orderStateMachineService) {
        this.orderRepository = orderRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.orderStateMachineService = orderStateMachineService;
    }

    @Transactional
    public void prepareForRelease(Integer tableId) {
        List<PaymentIntent> paymentIntents = paymentIntentRepository.findLockedByOrderTableId(tableId);
        if (paymentIntents.stream().map(PaymentIntent::getStatus)
                .anyMatch(BLOCKING_PAYMENT_STATUSES::contains)) {
            throw conflict("Bàn còn giao dịch thanh toán đang chờ xử lý");
        }
        List<Order> orders = orderRepository.findOrdersByTableIdWithDetails(tableId);
        for (Order order : orders) {
            if (inventoryReservationRepository.existsByOrderIdAndStatus(
                    order.getId(), InventoryReservationStatus.RESERVED)) {
                throw conflict("Bàn còn giữ chỗ nguyên liệu đang hoạt động");
            }
            OrderStatus status = orderStateMachineService.current(order);
            if (status == OrderStatus.CANCELLED) {
                if (blockingPayment(order)) throw conflict("Đơn đã hủy còn hoàn tiền hoặc số dư đang xử lý");
                continue;
            }
            if (status == OrderStatus.COMPLETED) {
                if (blockingPayment(order)) throw conflict("Đơn hoàn thành vẫn còn số dư đang xử lý");
                continue;
            }
            if (!Boolean.TRUE.equals(order.getIsPaid()) || positive(order.getRemainingAmount())) {
                throw conflict("Bàn còn hóa đơn chưa thanh toán đủ");
            }
            if (blockingPayment(order)) {
                throw conflict("Bàn còn trạng thái thanh toán hoặc hoàn tiền đang xử lý");
            }
            boolean hasUnservedDish = order.getOrderDetails() != null && order.getOrderDetails().stream()
                    .filter(detail -> !Integer.valueOf(3).equals(detail.getStatus()))
                    .anyMatch(detail -> !Integer.valueOf(2).equals(detail.getStatus()));
            if (hasUnservedDish) {
                throw conflict("Bàn còn món chưa được phục vụ xong");
            }
            orderStateMachineService.transition(order, OrderStatus.COMPLETED);
            orderRepository.save(order);
        }
    }

    private boolean blockingPayment(Order order) {
        return positive(order.getRemainingAmount())
                || (order.getPaymentStatus() != null
                        && BLOCKING_PAYMENT_STATUSES.contains(order.getPaymentStatus()));
    }

    private boolean positive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
