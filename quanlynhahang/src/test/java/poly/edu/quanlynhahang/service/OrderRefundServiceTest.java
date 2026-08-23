package poly.edu.quanlynhahang.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.PaymentDirection;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.RefundTransaction;
import poly.edu.quanlynhahang.entity.RefundTransaction.RefundStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;

@ExtendWith(MockitoExtension.class)
class OrderRefundServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock RefundTransactionRepository refundRepository;
    @Mock PaymentTransactionRepository paymentTransactionRepository;
    @Mock PointsLedgerService pointsLedgerService;
    @Mock ActivityLogService activityLogService;
    @Mock TableLifecycleService tableLifecycleService;
    @Mock InventoryReservationService inventoryReservationService;
    @Mock RefundService refundService;

    private OrderRefundService service;

    @BeforeEach
    void setUp() {
        service = new OrderRefundService(orderRepository, refundRepository, paymentTransactionRepository,
                pointsLedgerService, activityLogService, tableLifecycleService,
                inventoryReservationService, new OrderStateMachineService(), refundService);
        lenient().when(refundRepository.findByOrderIdOrderByCreatedAtDesc(1)).thenReturn(List.of());
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(refundRepository.save(any(RefundTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void unpaidCancellationCreatesNoRefundAndReleasesTable() {
        Order order = order(OrderPaymentOption.PAY_AT_RESTAURANT);
        order.setTableId(9);
        when(orderRepository.findLockedById(1)).thenReturn(Optional.of(order));

        var result = service.cancelAndRequestRefund(1, "cashier");

        assertThat(result.refundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(3);
        verify(tableLifecycleService).release(9);
        verify(inventoryReservationService).release(1,
                poly.edu.quanlynhahang.entity.InventoryReservationStatus.RELEASED);
        verify(refundRepository, never()).save(any());
    }

    @Test
    void prepaidPartialPaymentRefundsOnlySuccessfulLedgerCredits() {
        Order order = order(OrderPaymentOption.PREPAID_TRANSFER);
        PaymentTransaction credit = transaction(PaymentDirection.CREDIT, "40000");
        when(orderRepository.findLockedById(1)).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByAggregateTypeAndAggregateIdAndStatus(
                any(), any(), any())).thenReturn(List.of(credit));

        var result = service.cancelAndRequestRefund(1, "cashier");

        assertThat(result.refundAmount()).isEqualByComparingTo("40000");
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        ArgumentCaptor<RefundTransaction> refund = ArgumentCaptor.forClass(RefundTransaction.class);
        verify(refundRepository).save(refund.capture());
        assertThat(refund.getValue().getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.getValue().getAmount()).isEqualByComparingTo("40000");
        verify(tableLifecycleService, never()).release(any());
    }

    @Test
    void manuallyConfirmedPaymentUsesRecordedPaidAmount() {
        Order order = order(OrderPaymentOption.PAY_AT_RESTAURANT);
        order.setPaidAmount(new BigDecimal("125000"));
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentConfirmedAt(new Date());
        when(orderRepository.findLockedById(1)).thenReturn(Optional.of(order));

        var result = service.cancelAndRequestRefund(1, "cashier");

        assertThat(result.refundAmount()).isEqualByComparingTo("125000");
        assertThat(result.refundStatus()).isEqualTo(RefundStatus.PENDING);
    }

    @Test
    void retryReturnsExistingRefundWithoutCreatingDuplicate() {
        Order order = order(OrderPaymentOption.PREPAID_TRANSFER);
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        RefundTransaction existing = new RefundTransaction();
        existing.setAmount(new BigDecimal("80000"));
        existing.setStatus(RefundStatus.PENDING);
        existing.setCreatedAt(new Date());
        when(orderRepository.findLockedById(1)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(1)).thenReturn(List.of(existing));

        var result = service.cancelAndRequestRefund(1, "cashier");

        assertThat(result.idempotent()).isTrue();
        assertThat(result.refundAmount()).isEqualByComparingTo("80000");
        verify(refundRepository, never()).save(any());
        verify(paymentTransactionRepository, never())
                .findByAggregateTypeAndAggregateIdAndStatus(any(), any(), any());
    }

    @Test
    void confirmedRefundMarksOrderRefundedThenReleasesItsTable() {
        Order order = order(OrderPaymentOption.PREPAID_TRANSFER);
        order.setTableId(9);
        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        RefundTransaction pending = new RefundTransaction();
        pending.setId(20L);
        pending.setOrderId(1);
        pending.setAmount(new BigDecimal("80000"));
        pending.setStatus(RefundStatus.PENDING);
        pending.setCreatedAt(new Date());
        RefundTransaction completed = new RefundTransaction();
        completed.setId(20L);
        completed.setOrderId(1);
        completed.setAmount(new BigDecimal("80000"));
        completed.setStatus(RefundStatus.COMPLETED);
        when(orderRepository.findLockedById(1)).thenReturn(Optional.of(order));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(1)).thenReturn(List.of(pending));
        when(refundService.confirmCompleted(20L, "BANK-REF-1", "Đã đối soát", "cashier"))
                .thenReturn(completed);

        var result = service.completeRefund(1, "BANK-REF-1", "Đã đối soát", "cashier");

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.refundStatus()).isEqualTo(RefundStatus.COMPLETED);
        verify(tableLifecycleService).release(9);
    }

    private Order order(OrderPaymentOption option) {
        Order order = new Order();
        order.setId(1);
        order.setPaymentOption(option);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRemainingAmount(BigDecimal.ZERO);
        return order;
    }

    private PaymentTransaction transaction(PaymentDirection direction, String amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setDirection(direction);
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }
}
