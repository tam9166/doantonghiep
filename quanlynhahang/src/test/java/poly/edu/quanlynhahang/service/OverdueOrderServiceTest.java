package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OverdueOrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock ActivityLogService activityLogService;
    @Mock PointsLedgerService pointsLedgerService;

    private OverdueOrderService service;

    @BeforeEach
    void setUp() {
        service = new OverdueOrderService(orderRepository, new OrderStateMachineService(),
                activityLogService, pointsLedgerService);
    }

    @Test
    void completesOnlyPaidOverdueOrdersAndCreatesSystemAudit() {
        Order paid = order(11, true, PaymentStatus.PAID);
        Account customer = new Account();
        customer.setUsername("customer");
        paid.setAccount(customer);
        paid.setTotalAmount(new BigDecimal("150000"));
        Order unpaid = order(12, false, PaymentStatus.UNPAID);
        when(orderRepository.findOverdueOrders(anyList(), any(), any())).thenReturn(List.of(paid, unpaid));

        assertEquals(1, service.autoCompletePaidOverdueOrders());

        assertEquals(OrderStatus.COMPLETED.code(), paid.getStatus());
        assertEquals(OrderStatus.PENDING.code(), unpaid.getStatus());
        verify(orderRepository).save(paid);
        verify(orderRepository, never()).save(unpaid);
        verify(activityLogService).log(eq("SYSTEM_AUTO_UPDATE"), eq("Order"), eq("11"),
                contains("SYSTEM AUTO UPDATE"));
        verify(pointsLedgerService).credit("customer", PointsEventType.ORDER_COMPLETED,
                "ORDER_COMPLETED:11", 15, "Thưởng điểm đơn tự động hoàn tất #11");
    }

    private Order order(int id, boolean paid, PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(OrderStatus.PENDING.code());
        order.setIsPaid(paid);
        order.setPaymentStatus(paymentStatus);
        return order;
    }
}
