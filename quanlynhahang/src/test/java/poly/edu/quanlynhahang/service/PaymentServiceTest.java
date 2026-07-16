package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.config.PaymentProperties;
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
    private final PaymentProperties paymentProperties = paymentProperties();
    private final PaymentService service = new PaymentService(
            reservationRepository,
            paymentIntentRepository,
            realtimeService,
            stateMachine,
            paymentProperties);

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
    void createQrUsesConfiguredMbAccountWithoutDemoFallback() {
        Reservation reservation = pendingIntent().getReservation();
        when(reservationRepository.findByReservationCode("MV-001")).thenReturn(Optional.of(reservation));
        when(paymentIntentRepository.findByReservationIdAndStatusOrderByCreatedAtDesc(
                reservation.getId(), PaymentStatus.PENDING)).thenReturn(List.of());
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentQrRequest request = new PaymentQrRequest();
        request.setReservationCode("MV-001");
        request.setPaymentOption(PaymentOption.DEPOSIT_50);
        PaymentQrResponse response = service.createQr(request);

        assertEquals("MB", response.getBankCode());
        assertEquals("919112006789", response.getAccountNumber());
        assertEquals("HOANG NGUYEN MINH TAM", response.getAccountHolder());
        assertTrue(response.getQrUrl().contains("/MB-919112006789-compact2.png"));
    }

    @Test
    void twoReservationsWithSameAmountReceiveDifferentPaymentCodesAndTransferContent() {
        Reservation first = reservation(1L, "MV-001");
        Reservation second = reservation(2L, "MV-002");
        when(reservationRepository.findByReservationCode("MV-001")).thenReturn(Optional.of(first));
        when(reservationRepository.findByReservationCode("MV-002")).thenReturn(Optional.of(second));
        when(paymentIntentRepository.findByReservationIdAndStatusOrderByCreatedAtDesc(
                any(), any())).thenReturn(List.of());
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentQrRequest firstRequest = new PaymentQrRequest();
        firstRequest.setReservationCode("MV-001");
        firstRequest.setPaymentOption(PaymentOption.DEPOSIT_50);
        PaymentQrRequest secondRequest = new PaymentQrRequest();
        secondRequest.setReservationCode("MV-002");
        secondRequest.setPaymentOption(PaymentOption.DEPOSIT_50);

        PaymentQrResponse firstResponse = service.createQr(firstRequest);
        PaymentQrResponse secondResponse = service.createQr(secondRequest);

        assertEquals(firstResponse.getAmount(), secondResponse.getAmount());
        assertNotEquals(firstResponse.getPaymentCode(), secondResponse.getPaymentCode());
        assertNotEquals(firstResponse.getTransferContent(), secondResponse.getTransferContent());
        assertNotEquals(firstResponse.getQrUrl(), secondResponse.getQrUrl());
    }

    private PaymentIntent pendingIntent() {
        Reservation reservation = reservation(1L, "MV-001");

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

    private Reservation reservation(Long id, String code) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setReservationCode(code);
        reservation.setReservationStatus(ReservationStatus.PENDING);
        reservation.setPaymentStatus(PaymentStatus.PENDING);
        reservation.setTotalAmount(new BigDecimal("200000"));
        reservation.setDepositAmount(new BigDecimal("100000"));
        reservation.setRemainingAmount(new BigDecimal("200000"));
        return reservation;
    }

    private PaymentProperties paymentProperties() {
        PaymentProperties properties = new PaymentProperties();
        properties.setBankCode("MB");
        properties.setBankBin("970422");
        properties.setAccountNumber("919112006789");
        properties.setAccountHolder("HOANG NGUYEN MINH TAM");
        properties.setQrProvider("VIETQR");
        properties.setQrExpirationMinutes(15);
        properties.setDemoMode(false);
        return properties;
    }
}
