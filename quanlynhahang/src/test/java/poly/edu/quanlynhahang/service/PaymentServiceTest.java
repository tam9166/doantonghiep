package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.config.PaymentProperties;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.dto.PaymentQrRequest;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

class PaymentServiceTest {

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final PaymentIntentRepository paymentIntentRepository = mock(PaymentIntentRepository.class);
    private final ReservationRealtimeService realtimeService = mock(ReservationRealtimeService.class);
    private final ReservationStateMachine stateMachine = mock(ReservationStateMachine.class);
    private final PaymentProperties paymentProperties = paymentProperties();
    private final PaymentCapabilityService capabilityService = mock(PaymentCapabilityService.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final ReservationReceiptService receiptService = mock(ReservationReceiptService.class);
    private final RestaurantSettingsService settingsService = mock(RestaurantSettingsService.class);
    private final PaymentService service = new PaymentService(
            reservationRepository,
            paymentIntentRepository,
            realtimeService,
            stateMachine,
            paymentProperties,
            capabilityService,
            activityLogService,
            receiptService,
            settingsService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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
        assertEquals(PaymentStatus.PARTIALLY_PAID, intent.getReservation().getPaymentStatus());
        assertEquals(DepositStatus.PAID, intent.getReservation().getDepositStatus());
        assertEquals(new BigDecimal("100000"), intent.getReservation().getPaidAmount());
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
    void paidDepositCannotCreateAnotherDepositQr() {
        Reservation reservation = pendingIntent().getReservation();
        reservation.setPaidAmount(new BigDecimal("100000"));
        reservation.setDepositStatus(DepositStatus.PAID);
        reservation.setReservationStatus(ReservationStatus.DEPOSIT_PAID);
        when(reservationRepository.findLockedByReservationCode("MV-001")).thenReturn(Optional.of(reservation));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createQr(
                        request("MV-001", PaymentOption.DEPOSIT_50),
                        "capability",
                        "second-deposit-001"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(paymentIntentRepository, never()).save(any());
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
        when(reservationRepository.findLockedByReservationCode("MV-001")).thenReturn(Optional.of(reservation));
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentQrRequest request = new PaymentQrRequest();
        request.setReservationCode("MV-001");
        request.setPaymentOption(PaymentOption.DEPOSIT_50);
        PaymentQrResponse response = service.createQr(request, "capability-token", "create-qr-001");

        assertEquals("MB", response.getBankCode());
        assertEquals("1234567890", response.getAccountNumber());
        assertEquals("TEST ACCOUNT HOLDER", response.getAccountHolder());
        assertTrue(response.getQrUrl().contains("/MB-1234567890-compact2.png"));
    }

    @Test
    void twoReservationsWithSameAmountReceiveDifferentPaymentCodesAndTransferContent() {
        Reservation first = reservation(1L, "MV-001");
        Reservation second = reservation(2L, "MV-002");
        when(reservationRepository.findLockedByReservationCode("MV-001")).thenReturn(Optional.of(first));
        when(reservationRepository.findLockedByReservationCode("MV-002")).thenReturn(Optional.of(second));
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentQrRequest firstRequest = new PaymentQrRequest();
        firstRequest.setReservationCode("MV-001");
        firstRequest.setPaymentOption(PaymentOption.DEPOSIT_50);
        PaymentQrRequest secondRequest = new PaymentQrRequest();
        secondRequest.setReservationCode("MV-002");
        secondRequest.setPaymentOption(PaymentOption.DEPOSIT_50);

        PaymentQrResponse firstResponse = service.createQr(
                firstRequest, "first-capability-token", "create-first-001");
        PaymentQrResponse secondResponse = service.createQr(
                secondRequest, "second-capability-token", "create-second-001");

        assertEquals(firstResponse.getAmount(), secondResponse.getAmount());
        assertNotEquals(firstResponse.getPaymentCode(), secondResponse.getPaymentCode());
        assertNotEquals(firstResponse.getTransferContent(), secondResponse.getTransferContent());
        assertNotEquals(firstResponse.getQrUrl(), secondResponse.getQrUrl());
    }

    @Test
    void retryWithSameIdempotencyKeyReturnsSameIntentWithoutSavingAgain() {
        Reservation reservation = reservation(1L, "MV-001");
        AtomicReference<PaymentIntent> savedIntent = new AtomicReference<>();
        when(reservationRepository.findLockedByReservationCode("MV-001")).thenReturn(Optional.of(reservation));
        when(paymentIntentRepository.findByIdempotencyKey("same-key-001"))
                .thenAnswer(invocation -> Optional.ofNullable(savedIntent.get()));
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            intent.setId(20L);
            savedIntent.set(intent);
            return intent;
        });
        PaymentQrRequest request = request("MV-001", PaymentOption.DEPOSIT_50);

        PaymentQrResponse first = service.createQr(request, "capability", "same-key-001");
        PaymentQrResponse retry = service.createQr(request, "capability", "same-key-001");

        assertEquals(first.getPaymentCode(), retry.getPaymentCode());
        assertEquals(first.getQrUrl(), retry.getQrUrl());
        verify(paymentIntentRepository, times(1)).save(any());
    }

    @Test
    void sameIdempotencyKeyWithDifferentPayloadReturnsConflict() {
        Reservation reservation = reservation(1L, "MV-001");
        PaymentIntent existing = pendingIntent();
        existing.setRequestHash("different-request-hash");
        when(reservationRepository.findLockedByReservationCode("MV-001")).thenReturn(Optional.of(reservation));
        when(paymentIntentRepository.findByIdempotencyKey("conflict-key-001")).thenReturn(Optional.of(existing));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createQr(
                        request("MV-001", PaymentOption.DEPOSIT_50),
                        "capability",
                        "conflict-key-001"));

        assertEquals("409 CONFLICT \"IDEMPOTENCY_CONFLICT\"", error.getMessage());
    }

    @Test
    void regenerateReplacesOldIntentWithoutCreatingReservation() {
        PaymentIntent existing = pendingIntent();
        Reservation reservation = existing.getReservation();
        when(paymentIntentRepository.findByPaymentCode("PAY-MV-001")).thenReturn(Optional.of(existing));
        when(paymentIntentRepository.findLockedByPaymentCode("PAY-MV-001")).thenReturn(Optional.of(existing));
        when(reservationRepository.findLockedByReservationCode("MV-001")).thenReturn(Optional.of(reservation));
        when(paymentIntentRepository.saveAndFlush(existing)).thenReturn(existing);
        when(paymentIntentRepository.save(any())).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            if (intent.getId() == null) intent.setId(11L);
            return intent;
        });

        PaymentQrResponse replacement = service.regenerate(
                "PAY-MV-001", "capability", "regenerate-key-001");

        assertEquals(PaymentStatus.REPLACED, existing.getStatus());
        assertEquals(11L, existing.getReplacedById());
        assertNotEquals("PAY-MV-001", replacement.getPaymentCode());
        verify(reservationRepository, never()).save(any());
        verify(activityLogService).log(
                "CREATE_PAYMENT_QR", "PaymentIntent", replacement.getPaymentCode(), "Tạo QR cho MV-001");
        verify(activityLogService).log(
                "REGENERATE_QR", "PaymentIntent", "10", "Tạo lại QR cho MV-001");
    }

    @Test
    void strangerWhoKnowsPaymentCodeCannotReadQr() {
        PaymentIntent intent = pendingIntent();
        intent.getReservation().setCreatedBy("owner01");
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("stranger01", null, "ROLE_CUSTOMER"));
        when(paymentIntentRepository.findByPaymentCode("PAY-MV-001")).thenReturn(Optional.of(intent));
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED"))
                .when(capabilityService).authorizePaymentQr(intent.getReservation(), null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.getPayment("PAY-MV-001", null));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(paymentIntentRepository, never()).save(any());
    }

    private PaymentQrRequest request(String reservationCode, PaymentOption option) {
        PaymentQrRequest request = new PaymentQrRequest();
        request.setReservationCode(reservationCode);
        request.setPaymentOption(option);
        return request;
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
        intent.setAccountNumber("1234567890");
        intent.setAccountHolder("TEST ACCOUNT HOLDER");
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
        properties.setAccountNumber("1234567890");
        properties.setAccountHolder("TEST ACCOUNT HOLDER");
        properties.setQrProvider("VIETQR");
        properties.setQrExpirationMinutes(15);
        properties.setDemoMode(false);
        return properties;
    }
}
