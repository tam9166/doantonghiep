package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

@SpringBootTest
class TableReleaseConcurrencyIntegrationTest {
    @Autowired TableLifecycleService tableLifecycleService;
    @Autowired PaymentLedgerService paymentLedgerService;
    @Autowired RestaurantTableRepository tableRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentIntentRepository paymentIntentRepository;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean TableSessionService tableSessionService;
    @MockitoBean InventoryReservationService inventoryReservationService;
    @MockitoBean ActivityLogService activityLogService;
    @MockitoBean ReservationRealtimeService reservationRealtimeService;
    @MockitoBean SimpMessagingTemplate messagingTemplate;

    private Integer tableId;
    private Integer orderId;
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
        if (orderId != null) {
            jdbc.update("DELETE FROM Orders WHERE id = ?", orderId);
        }
        if (tableId != null) {
            jdbc.update("DELETE FROM restaurant_table WHERE id = ?", tableId);
        }
    }

    @Test
    @Timeout(30)
    void twoConcurrentReleasesAreIdempotentAndRevokeCapabilityOnce() throws Exception {
        RestaurantTable table = table(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RestaurantTable> first = executor.submit(() -> releaseAfter(start));
            Future<RestaurantTable> second = executor.submit(() -> releaseAfter(start));
            start.countDown();

            assertEquals(0, first.get(20, TimeUnit.SECONDS).getIsOccupied());
            assertEquals(0, second.get(20, TimeUnit.SECONDS).getIsOccupied());
            assertEquals(0, jdbc.queryForObject(
                    "SELECT is_occupied FROM restaurant_table WHERE id = ?", Integer.class, table.getId()));
            verify(tableSessionService, times(1)).revokeActiveForTable(table.getId());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    void releaseWaitsForPaymentCallbackCommitAndEvaluatesTheCommittedState() throws Exception {
        RestaurantTable table = table(2);
        Order order = unpaidOrder(table.getId());
        PaymentIntent intent = paymentIntent(order);
        CountDownLatch callbackApplied = new CountDownLatch(1);
        CountDownLatch allowCallbackCommit = new CountDownLatch(1);
        AtomicReference<PaymentLedgerResult> callbackResult = new AtomicReference<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> callback = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                callbackResult.set(paymentLedgerService.recordCredit(
                        "bank", providerTransactionId, intent.getPaymentCode(), "TT TABLE RELEASE",
                        new BigDecimal("100000"), "1234567890", "release-race-payload"));
                callbackApplied.countDown();
                await(allowCallbackCommit);
            }));
            callbackApplied.await(10, TimeUnit.SECONDS);

            Future<RestaurantTable> release = executor.submit(() -> tableLifecycleService.release(table.getId()));
            assertThrows(TimeoutException.class, () -> release.get(500, TimeUnit.MILLISECONDS),
                    "Release must wait while the callback transaction owns the payment-intent lock");

            allowCallbackCommit.countDown();
            callback.get(20, TimeUnit.SECONDS);
            assertEquals("PAYMENT_PAID", callbackResult.get().code());
            assertEquals(0, release.get(20, TimeUnit.SECONDS).getIsOccupied());
            assertEquals(PaymentStatus.PAID.name(), jdbc.queryForObject(
                    "SELECT payment_status FROM Orders WHERE id = ?", String.class, order.getId()));
            assertEquals(4, jdbc.queryForObject(
                    "SELECT status FROM Orders WHERE id = ?", Integer.class, order.getId()));
            verify(tableSessionService).revokeActiveForTable(table.getId());
        } finally {
            allowCallbackCommit.countDown();
            executor.shutdownNow();
        }
    }

    private RestaurantTable releaseAfter(CountDownLatch start) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        return tableLifecycleService.release(tableId);
    }

    private RestaurantTable table(int occupied) {
        RestaurantTable table = new RestaurantTable();
        table.setName("REG-RELEASE-" + marker());
        table.setFloor("REGRESSION");
        table.setIsOccupied(occupied);
        RestaurantTable saved = tableRepository.saveAndFlush(table);
        tableId = saved.getId();
        return saved;
    }

    private Order unpaidOrder(Integer currentTableId) {
        Order order = new Order();
        order.setOrderCode("REG-ORDER-" + marker());
        order.setOrderType(OrderType.DINE_IN);
        order.setTableId(currentTableId);
        order.setStatus(0);
        order.setPaymentOption(OrderPaymentOption.PREPAID_TRANSFER);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setIsPaid(false);
        order.setTotalAmount(new BigDecimal("100000"));
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRemainingAmount(new BigDecimal("100000"));
        Order saved = orderRepository.saveAndFlush(order);
        orderId = saved.getId();
        return saved;
    }

    private PaymentIntent paymentIntent(Order order) {
        paymentCode = "PAY-REL-" + marker();
        providerTransactionId = "TX-REL-" + marker();
        PaymentIntent intent = new PaymentIntent();
        intent.setOrder(order);
        intent.setAggregateType("ORDER");
        intent.setAggregateId(order.getId().longValue());
        intent.setAggregateCode(order.getOrderCode());
        intent.setPurpose("FULL");
        intent.setPaymentCode(paymentCode);
        intent.setPaymentOption(PaymentOption.FULL);
        intent.setStatus(PaymentStatus.PARTIALLY_PAID);
        intent.setAmount(new BigDecimal("100000"));
        intent.setPaidAmount(BigDecimal.ZERO);
        intent.setRemainingAmount(new BigDecimal("100000"));
        intent.setBankCode("TEST");
        intent.setAccountNumber("1234567890");
        intent.setAccountHolder("REGRESSION TEST");
        intent.setQrProvider("TEST");
        intent.setTransferContent("TT TABLE RELEASE");
        intent.setQrUrl("https://example.invalid/table-release-race");
        intent.setExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)));
        return paymentIntentRepository.saveAndFlush(intent);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating test", exception);
        }
    }

    private String marker() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
