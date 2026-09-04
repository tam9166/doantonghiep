package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.PaymentDirection;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;
import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.RefundTransaction;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

class RefundServiceTest {
    private final RefundTransactionRepository refundRepository = mock(RefundTransactionRepository.class);
    private final PaymentIntentRepository intentRepository = mock(PaymentIntentRepository.class);
    private final PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
    private final DepositPolicyService depositPolicyService = mock(DepositPolicyService.class);
    private RefundService service;

    @BeforeEach
    void setUp() {
        when(refundRepository.save(any())).thenAnswer(invocation -> {
            RefundTransaction value = invocation.getArgument(0);
            if (value.getId() == null) value.setId(20L);
            return value;
        });
        service = new RefundService(refundRepository, intentRepository,
                mock(ReservationRepository.class), depositPolicyService,
                mock(ActivityLogService.class), transactionRepository);
    }

    @Test
    void creatingReservationRefundNeverFakesProviderCompletion() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationCode("RES-A");
        reservation.setPaidAmount(new BigDecimal("500000"));
        when(depositPolicyService.calculateNoShowForfeiture(reservation)).thenReturn(new BigDecimal("250000"));
        when(intentRepository.findByReservationIdAndStatusOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());

        RefundTransaction result = service.processReservationRefund(
                reservation, RefundTransaction.RefundReason.CANCELLED_BY_CUSTOMER, "Khách hủy", "admin");

        assertEquals(RefundTransaction.RefundStatus.PENDING, result.getStatus());
    }

    @Test
    void completionRequiresARealReferenceAndWritesRefundLedgerDirection() {
        RefundTransaction refund = new RefundTransaction();
        refund.setId(20L);
        refund.setReservationId(1L);
        refund.setAmount(new BigDecimal("250000"));
        refund.setStatus(RefundTransaction.RefundStatus.PENDING);
        when(refundRepository.findLockedById(20L)).thenReturn(Optional.of(refund));
        when(transactionRepository.findByProviderTransactionId("BANK-REF-1")).thenReturn(Optional.empty());

        RefundTransaction result = service.confirmCompleted(20L, "BANK-REF-1", "Đã đối soát", "cashier");

        assertEquals(RefundTransaction.RefundStatus.COMPLETED, result.getStatus());
        verify(transactionRepository).save(org.mockito.ArgumentMatchers.argThat(
                transaction -> PaymentDirection.REFUND.equals(((PaymentTransaction) transaction).getDirection())));
    }

    @Test
    void completionWithoutManualReferenceUsesTheDatabaseRefundIdAsTheStableReference() {
        RefundTransaction refund = new RefundTransaction();
        refund.setId(20L);
        refund.setReservationId(1L);
        refund.setAmount(new BigDecimal("250000"));
        refund.setStatus(RefundTransaction.RefundStatus.PENDING);
        when(refundRepository.findLockedById(20L)).thenReturn(Optional.of(refund));
        when(transactionRepository.findByProviderTransactionId("REFUND-20")).thenReturn(Optional.empty());

        service.confirmCompleted(20L, null, "Đã đối soát", "cashier");

        verify(transactionRepository).save(org.mockito.ArgumentMatchers.argThat(
                transaction -> "REFUND-20".equals(((PaymentTransaction) transaction).getProviderTransactionId())));
    }

    @Test
    void completedRefundAcceptsOnlyTheExactMatchingLedgerReference() {
        RefundTransaction refund = completedOrderRefund();
        PaymentTransaction recorded = new PaymentTransaction();
        recorded.setAggregateType("ORDER");
        recorded.setAggregateId(7L);
        recorded.setAmount(new BigDecimal("80000"));
        recorded.setDirection(PaymentDirection.REFUND);
        recorded.setStatus(PaymentTransactionStatus.SUCCESS);
        when(refundRepository.findLockedById(20L)).thenReturn(Optional.of(refund));
        when(transactionRepository.findByProviderTransactionId("BANK-REF-7"))
                .thenReturn(Optional.of(recorded));

        assertEquals(refund, service.confirmCompleted(20L, "BANK-REF-7", null, "cashier"));
    }

    @Test
    void completedRefundRejectsAChangedProviderReference() {
        when(refundRepository.findLockedById(20L)).thenReturn(Optional.of(completedOrderRefund()));
        when(transactionRepository.findByProviderTransactionId("DIFFERENT-REF"))
                .thenReturn(Optional.empty());

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.confirmCompleted(20L, "DIFFERENT-REF", null, "cashier"));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void completedLegacyRefundCanBeReadIdempotentlyWithoutSubmittingItsOldReference() {
        RefundTransaction refund = completedOrderRefund();
        PaymentTransaction recorded = new PaymentTransaction();
        recorded.setAggregateType("ORDER");
        recorded.setAggregateId(7L);
        recorded.setAmount(new BigDecimal("80000"));
        recorded.setDirection(PaymentDirection.REFUND);
        recorded.setStatus(PaymentTransactionStatus.SUCCESS);
        when(refundRepository.findLockedById(20L)).thenReturn(Optional.of(refund));
        when(transactionRepository.findByAggregateTypeAndAggregateIdAndStatus(
                "ORDER", 7L, PaymentTransactionStatus.SUCCESS)).thenReturn(List.of(recorded));

        assertEquals(refund, service.confirmCompleted(20L, null, null, "cashier"));
    }

    private RefundTransaction completedOrderRefund() {
        RefundTransaction refund = new RefundTransaction();
        refund.setId(20L);
        refund.setOrderId(7);
        refund.setAmount(new BigDecimal("80000"));
        refund.setStatus(RefundTransaction.RefundStatus.COMPLETED);
        return refund;
    }
}
