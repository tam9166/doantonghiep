package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

@SpringBootTest
class PaymentConcurrencyIntegrationTest {
    @Autowired PaymentLedgerService paymentLedgerService;
    @Autowired PaymentIntentRepository paymentIntentRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean ReservationRealtimeService realtimeService;
    @MockitoBean ActivityLogService activityLogService;
    @MockitoBean OrderPaymentService orderPaymentService;

    private String reservationCode;
    private String paymentCode;
    private String providerTransactionId;

    @AfterEach
    void cleanup() {
        if (providerTransactionId != null) {
            jdbc.update("DELETE FROM payment_transactions WHERE provider_transaction_id = ?", providerTransactionId);
        }
        if (paymentCode != null) {
            jdbc.update("DELETE FROM payment_intents WHERE payment_code = ?", paymentCode);
        }
        if (reservationCode != null) {
            jdbc.update("DELETE FROM reservation_status_history WHERE reservation_id IN "
                    + "(SELECT id FROM reservations WHERE reservation_code = ?)", reservationCode);
            jdbc.update("DELETE FROM reservations WHERE reservation_code = ?", reservationCode);
        }
    }

    @Test
    @Timeout(30)
    void duplicateConcurrentCreditsAreIdempotentAndCreateOneLedgerEntry() throws Exception {
        createFixture();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<PaymentAttempt> first = executor.submit(() -> creditAfter(start));
            Future<PaymentAttempt> second = executor.submit(() -> creditAfter(start));
            start.countDown();

            List<PaymentAttempt> outcomes = List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS));
            outcomes.forEach(outcome -> assertNull(outcome.failure(),
                    () -> outcome.failure() == null ? "" : outcome.failure().toString()));
            assertEquals(1, outcomes.stream()
                    .filter(outcome -> "PAYMENT_PAID".equals(outcome.code())).count());
            assertEquals(1, outcomes.stream()
                    .filter(outcome -> "PAYMENT_ALREADY_PROCESSED".equals(outcome.code())).count());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM payment_transactions WHERE provider_transaction_id = ?",
                    Integer.class, providerTransactionId));
            assertEquals(PaymentTransactionStatus.SUCCESS.name(), jdbc.queryForObject(
                    "SELECT status FROM payment_transactions WHERE provider_transaction_id = ?",
                    String.class, providerTransactionId));
            assertEquals(new BigDecimal("100000"), jdbc.queryForObject(
                    "SELECT paid_amount FROM reservations WHERE reservation_code = ?",
                    BigDecimal.class, reservationCode));
        } finally {
            executor.shutdownNow();
        }
    }

    private PaymentAttempt creditAfter(CountDownLatch start) {
        try {
            start.await(10, TimeUnit.SECONDS);
            PaymentLedgerResult result = paymentLedgerService.recordCredit(
                    "bank", providerTransactionId, paymentCode, "TT REGRESSION",
                    new BigDecimal("100000"), "1234567890", "concurrency-payload");
            return new PaymentAttempt(result.code(), null);
        } catch (Throwable failure) {
            return new PaymentAttempt(null, failure);
        }
    }

    private void createFixture() {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        reservationCode = "REG-PAY-" + marker;
        paymentCode = "PAY-REG-" + marker;
        providerTransactionId = "TX-REG-" + marker;

        Reservation reservation = new Reservation();
        reservation.setReservationCode(reservationCode);
        reservation.setCustomerName("Payment concurrency regression");
        reservation.setCustomerPhone("0900000000");
        reservation.setReservationDate(LocalDate.now().plusDays(5));
        reservation.setArrivalTime(LocalTime.of(18, 0));
        reservation.setGuestCount(2);
        reservation.setReservationStatus(ReservationStatus.DEPOSIT_PENDING);
        reservation.setTotalAmount(new BigDecimal("100000"));
        reservation.setDepositAmount(new BigDecimal("50000"));
        reservation.setPaidAmount(BigDecimal.ZERO);
        reservation.setRemainingAmount(new BigDecimal("100000"));
        reservation.setDepositStatus(DepositStatus.PENDING);
        reservation.setPaymentOption(PaymentOption.FULL);
        reservation.setPaymentStatus(PaymentStatus.UNPAID);
        reservation = reservationRepository.saveAndFlush(reservation);

        PaymentIntent intent = new PaymentIntent();
        intent.setReservation(reservation);
        intent.setAggregateType("RESERVATION");
        intent.setAggregateId(reservation.getId());
        intent.setAggregateCode(reservationCode);
        intent.setPurpose("FULL");
        intent.setPaymentCode(paymentCode);
        intent.setPaymentOption(PaymentOption.FULL);
        intent.setStatus(PaymentStatus.PENDING);
        intent.setAmount(new BigDecimal("100000"));
        intent.setPaidAmount(BigDecimal.ZERO);
        intent.setRemainingAmount(new BigDecimal("100000"));
        intent.setBankCode("TEST");
        intent.setAccountNumber("1234567890");
        intent.setAccountHolder("REGRESSION TEST");
        intent.setQrProvider("TEST");
        intent.setTransferContent("TT REGRESSION");
        intent.setQrUrl("https://example.invalid/regression-qr");
        intent.setExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)));
        paymentIntentRepository.saveAndFlush(intent);
    }

    private record PaymentAttempt(String code, Throwable failure) {
    }
}
