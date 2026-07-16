package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

class PaymentLedgerServiceTest {

    private final PaymentIntentRepository intentRepository = mock(PaymentIntentRepository.class);
    private final PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ReservationStateMachine stateMachine = mock(ReservationStateMachine.class);
    private final ReservationRealtimeService realtimeService = mock(ReservationRealtimeService.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final PaymentLedgerService service = new PaymentLedgerService(
            intentRepository,
            transactionRepository,
            reservationRepository,
            stateMachine,
            realtimeService,
            activityLogService);

    private PaymentIntent intent;
    private AtomicReference<PaymentTransaction> savedTransaction;

    @BeforeEach
    void setUp() {
        intent = intent();
        savedTransaction = new AtomicReference<>();
        when(transactionRepository.findByProviderTransactionId(any())).thenReturn(Optional.empty());
        when(intentRepository.findLockedByPaymentCode("PAY-001")).thenReturn(Optional.of(intent));
        when(transactionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PaymentTransaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            savedTransaction.set(transaction);
            return transaction;
        });
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            PaymentTransaction transaction = invocation.getArgument(0);
            savedTransaction.set(transaction);
            return transaction;
        });
        when(transactionRepository.findByPaymentIntentIdAndStatus(10L, PaymentTransactionStatus.SUCCESS))
                .thenAnswer(invocation -> List.of(savedTransaction.get()));
        when(transactionRepository.findByAggregateTypeAndAggregateIdAndStatus(
                "RESERVATION", 1L, PaymentTransactionStatus.SUCCESS))
                .thenAnswer(invocation -> List.of(savedTransaction.get()));
    }

    @Test
    void depositCreditMarksIntentPaidButReservationOnlyPartiallyPaid() {
        PaymentLedgerResult result = record(new BigDecimal("100000"), "TX-001");

        assertEquals("PAYMENT_PARTIALLY_PAID", result.code());
        assertEquals(PaymentStatus.PAID, intent.getStatus());
        assertEquals(PaymentStatus.PARTIALLY_PAID, intent.getReservation().getPaymentStatus());
        assertEquals(new BigDecimal("100000"), intent.getReservation().getPaidAmount());
        assertEquals(new BigDecimal("100000"), intent.getReservation().getRemainingAmount());
        assertEquals(DepositStatus.PAID, intent.getReservation().getDepositStatus());
        assertEquals(ReservationStatus.DEPOSIT_PAID, intent.getReservation().getReservationStatus());
    }

    @Test
    void insufficientCreditStaysPartiallyPaidWithoutClaimingDepositPaid() {
        PaymentLedgerResult result = record(new BigDecimal("50000"), "TX-002");

        assertEquals("PAYMENT_PARTIALLY_PAID", result.code());
        assertEquals(PaymentStatus.PARTIALLY_PAID, intent.getStatus());
        assertEquals(PaymentStatus.PARTIALLY_PAID, intent.getReservation().getPaymentStatus());
        assertEquals(DepositStatus.PENDING, intent.getReservation().getDepositStatus());
        assertEquals(ReservationStatus.PENDING, intent.getReservation().getReservationStatus());
    }

    @Test
    void excessCreditIsOverpaidAndNeverCollapsedToPaid() {
        PaymentLedgerResult result = record(new BigDecimal("250000"), "TX-003");

        assertEquals("PAYMENT_OVERPAID", result.code());
        assertEquals(PaymentStatus.OVERPAID, intent.getStatus());
        assertEquals(PaymentStatus.OVERPAID, intent.getReservation().getPaymentStatus());
        assertEquals(BigDecimal.ZERO, intent.getReservation().getRemainingAmount());
        assertEquals(ReservationStatus.FULLY_PAID, intent.getReservation().getReservationStatus());
    }

    @Test
    void duplicateProviderTransactionIsIdempotent() {
        PaymentTransaction existing = new PaymentTransaction();
        when(transactionRepository.findByProviderTransactionId("TX-DUP")).thenReturn(Optional.of(existing));

        PaymentLedgerResult result = record(new BigDecimal("100000"), "TX-DUP");

        assertEquals("PAYMENT_ALREADY_PROCESSED", result.code());
        verify(transactionRepository, never()).saveAndFlush(any());
        verify(intentRepository, never()).save(any());
    }

    @Test
    void transferContentMismatchGoesToManualReviewWithoutUpdatingBill() {
        PaymentLedgerResult result = service.recordCredit(
                "bank", "TX-004", "PAY-001", "WRONG CONTENT",
                new BigDecimal("100000"), "919112006789", "payload-hash");

        assertEquals("PAYMENT_MANUAL_REVIEW", result.code());
        assertEquals(PaymentTransactionStatus.MANUAL_REVIEW, savedTransaction.get().getStatus());
        verify(intentRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    private PaymentLedgerResult record(BigDecimal amount, String transactionId) {
        return service.recordCredit(
                "bank", transactionId, "PAY-001", "TT MV001 ABC12345",
                amount, "919112006789", "payload-hash");
    }

    private PaymentIntent intent() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationCode("MV-001");
        reservation.setTotalAmount(new BigDecimal("200000"));
        reservation.setDepositAmount(new BigDecimal("100000"));
        reservation.setPaidAmount(BigDecimal.ZERO);
        reservation.setRemainingAmount(new BigDecimal("200000"));
        reservation.setDepositStatus(DepositStatus.PENDING);
        reservation.setPaymentStatus(PaymentStatus.UNPAID);
        reservation.setReservationStatus(ReservationStatus.PENDING);

        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId(10L);
        paymentIntent.setReservation(reservation);
        paymentIntent.setAggregateType("RESERVATION");
        paymentIntent.setAggregateId(1L);
        paymentIntent.setPaymentCode("PAY-001");
        paymentIntent.setAmount(new BigDecimal("100000"));
        paymentIntent.setPaidAmount(BigDecimal.ZERO);
        paymentIntent.setRemainingAmount(new BigDecimal("100000"));
        paymentIntent.setStatus(PaymentStatus.PENDING);
        paymentIntent.setAccountNumber("919112006789");
        paymentIntent.setTransferContent("TT MV001 ABC12345");
        return paymentIntent;
    }
}
