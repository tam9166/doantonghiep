package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.dto.PaymentQrRequest;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

class PaymentServiceTest {

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final PaymentIntentRepository paymentIntentRepository = mock(PaymentIntentRepository.class);
    private final ReservationRealtimeService realtimeService = mock(ReservationRealtimeService.class);
    private final ReservationStateMachine stateMachine = mock(ReservationStateMachine.class);
    private final PaymentService service = new PaymentService(
            reservationRepository,
            paymentIntentRepository,
            realtimeService,
            stateMachine,
            "MB",
            "919112006789",
            "Hoang Nguyen Minh Tam",
            15);

    @Test
    void confirmFromWebhookMarksDepositPaidAndPublishesRealtimeEvent() {
        PaymentIntent intent = pendingIntent();
        when(paymentIntentRepository.findByPaymentCode("PAY-MV-001")).thenReturn(Optional.of(intent));
        when(paymentIntentRepository.findByBankTransactionCode("BANK-001")).thenReturn(Optional.empty());
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentQrResponse response = service.confirmFromWebhook("PAY-MV-001", "BANK-001", new BigDecimal("100000"));

        assertEquals(PaymentStatus.PAID, response.getStatus());
        assertEquals("BANK-001", intent.getBankTransactionCode());
        assertEquals(ReservationStatus.DEPOSIT_PAID, intent.getReservation().getReservationStatus());
        assertEquals(new BigDecimal("100000"), intent.getReservation().getRemainingAmount());
        verify(reservationRepository).save(intent.getReservation());
        verify(realtimeService).publish(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmFromWebhookRejectsExpiredQrAndPersistsExpiredStatus() {
        PaymentIntent intent = pendingIntent();
        intent.setExpiresAt(Date.from(Instant.now().minusSeconds(60)));
        when(paymentIntentRepository.findByPaymentCode("PAY-MV-001")).thenReturn(Optional.of(intent));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.confirmFromWebhook("PAY-MV-001", "BANK-001", new BigDecimal("100000")));

        assertEquals("409 CONFLICT \"PAYMENT_QR_EXPIRED\"", ex.getMessage());
        assertEquals(PaymentStatus.EXPIRED, intent.getStatus());
        verify(paymentIntentRepository).save(intent);
    }

    @Test
    void confirmFromWebhookRejectsAmountMismatch() {
        PaymentIntent intent = pendingIntent();
        when(paymentIntentRepository.findByPaymentCode("PAY-MV-001")).thenReturn(Optional.of(intent));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.confirmFromWebhook("PAY-MV-001", "BANK-001", new BigDecimal("90000")));

        assertEquals("422 UNPROCESSABLE_ENTITY \"PAYMENT_AMOUNT_MISMATCH\"", ex.getMessage());
    }

    @Test
    void confirmFromWebhookRejectsDuplicateBankTransactionCode() {
        PaymentIntent intent = pendingIntent();
        when(paymentIntentRepository.findByPaymentCode("PAY-MV-001")).thenReturn(Optional.of(intent));
        when(paymentIntentRepository.findByBankTransactionCode("BANK-001")).thenReturn(Optional.of(new PaymentIntent()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.confirmFromWebhook("PAY-MV-001", "BANK-001", new BigDecimal("100000")));

        assertEquals("409 CONFLICT \"PAYMENT_TRANSACTION_DUPLICATED\"", ex.getMessage());
    }

    @Test
    void createQrUsesDemoAccountWhenLocalBankConfigurationIsBlank() {
        PaymentService localService = new PaymentService(
                reservationRepository,
                paymentIntentRepository,
                realtimeService,
                stateMachine,
                "MB",
                "",
                "",
                15);
        Reservation reservation = pendingIntent().getReservation();
        when(reservationRepository.findByReservationCode("MV-001")).thenReturn(Optional.of(reservation));
        when(paymentIntentRepository.findByReservationIdAndStatusOrderByCreatedAtDesc(
                reservation.getId(), PaymentStatus.PENDING)).thenReturn(List.of());
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentQrRequest request = new PaymentQrRequest();
        request.setReservationCode("MV-001");
        request.setPaymentOption(PaymentOption.DEPOSIT_50);
        PaymentQrResponse response = localService.createQr(request);

        assertEquals("0000000000", response.getAccountNumber());
        assertEquals("Demo Restaurant", response.getAccountHolder());
        assertTrue(response.getQrUrl().contains("/MB-0000000000-compact2.png"));
        assertFalse(response.getQrUrl().contains("/MB--compact2.png"));
    }

    private PaymentIntent pendingIntent() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setReservationCode("MV-001");
        reservation.setReservationStatus(ReservationStatus.PENDING);
        reservation.setPaymentStatus(PaymentStatus.PENDING);
        reservation.setTotalAmount(new BigDecimal("200000"));
        reservation.setDepositAmount(new BigDecimal("100000"));
        reservation.setRemainingAmount(new BigDecimal("200000"));

        PaymentIntent intent = new PaymentIntent();
        intent.setId(10L);
        intent.setReservation(reservation);
        intent.setPaymentCode("PAY-MV-001");
        intent.setPaymentOption(PaymentOption.DEPOSIT_50);
        intent.setStatus(PaymentStatus.PENDING);
        intent.setAmount(new BigDecimal("100000"));
        intent.setBankCode("MB");
        intent.setAccountNumber("919112006789");
        intent.setAccountHolder("Hoang Nguyen Minh Tam");
        intent.setTransferContent("MV MV-001 PAY-MV-001");
        intent.setQrUrl("https://example.test/qr.png");
        intent.setExpiresAt(Date.from(Instant.now().plusSeconds(300)));
        return intent;
    }
}
