package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.config.PaymentProperties;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPaymentServiceTest {
    private final PaymentIntentRepository intentRepository = mock(PaymentIntentRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
    private final PaymentProperties properties = properties();
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final OrderServiceDateGuardService serviceDateGuard = mock(OrderServiceDateGuardService.class);
    private final OrderPaymentService service = new OrderPaymentService(
            intentRepository, orderRepository, properties, activityLogService, messagingTemplate, tableRepository,
            inventoryReservationService, new OrderStateMachineService(), serviceDateGuard);

    @BeforeEach
    void setUp() {
        clearInvocations(intentRepository, orderRepository, activityLogService, messagingTemplate,
                tableRepository, inventoryReservationService, serviceDateGuard);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(intentRepository.save(any())).thenAnswer(invocation -> {
            PaymentIntent intent = invocation.getArgument(0);
            intent.setId(7L);
            return intent;
        });
    }

    @Test
    void createsOrderIntentFromServerTotalAndBankConfiguration() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.UNPAID, 216_000.0);
        when(intentRepository.findFirstByOrderIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
                12, PaymentOption.FULL, PaymentStatus.PENDING)).thenReturn(Optional.empty());

        PaymentQrResponse response = service.createForOrder(order);

        assertEquals(new BigDecimal("216000"), response.getAmount());
        assertEquals("MB", response.getBankCode());
        assertEquals("1234567890", response.getAccountNumber());
        assertEquals("Hoàng Nguyễn Minh Tâm", response.getAccountHolder());
        assertEquals("Hoàng Nguyễn Minh Tâm", response.getAccountHolder());
        assertTrue(response.getTransferContent().startsWith("TT DH12 "));
        assertTrue(response.getQrUrl().contains("amount=216000"));
        verify(intentRepository).save(any(PaymentIntent.class));
    }

    @Test
    void cashierCreatesServerQrAndSwitchesOrderToLedgerPayment() {
        Order order = order(12, OrderPaymentOption.PAY_AT_RESTAURANT, PaymentStatus.UNPAID, 216_000.0);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));
        when(intentRepository.findFirstByOrderIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
                12, PaymentOption.FULL, PaymentStatus.PENDING)).thenReturn(Optional.empty());

        PaymentQrResponse response = service.createForExistingOrder(12);

        assertEquals(OrderPaymentOption.PREPAID_TRANSFER, order.getPaymentOption());
        assertEquals(new BigDecimal("216000"), response.getAmount());
        assertEquals("1234567890", response.getAccountNumber());
        verify(orderRepository).save(order);
        verify(intentRepository).save(any(PaymentIntent.class));
    }

    @Test
    void reservationDepositIsSubtractedOnceWhenGeneratingFinalQr() {
        Order order = order(12, OrderPaymentOption.PAY_AT_RESTAURANT, PaymentStatus.PARTIALLY_PAID, 700_000.0);
        order.setDeposit(new BigDecimal("500000"));
        order.setPaidAmount(new BigDecimal("500000"));
        order.setRemainingAmount(new BigDecimal("200000"));
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));
        when(intentRepository.findFirstByOrderIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
                12, PaymentOption.FULL, PaymentStatus.PENDING)).thenReturn(Optional.empty());

        PaymentQrResponse response = service.createForExistingOrder(12);

        assertEquals(new BigDecimal("200000"), response.getAmount());
        assertEquals(new BigDecimal("500000"), order.getPaidAmount());
        assertEquals(new BigDecimal("200000"), order.getRemainingAmount());
    }

    @Test
    void reusesUnexpiredOrderQrInsteadOfCreatingDuplicateIntent() {
        Order order = order(12, OrderPaymentOption.PAY_AT_RESTAURANT, PaymentStatus.UNPAID, 216_000.0);
        PaymentIntent active = new PaymentIntent();
        active.setOrder(order);
        active.setPaymentCode("PAY-EXISTING");
        active.setPaymentOption(PaymentOption.FULL);
        active.setStatus(PaymentStatus.PENDING);
        active.setAmount(new BigDecimal("216000"));
        active.setBankCode("MB");
        active.setAccountNumber("1234567890");
        active.setAccountHolder("TEST ACCOUNT HOLDER");
        active.setTransferContent("TT DH12 EXISTING");
        active.setExpiresAt(java.util.Date.from(java.time.Instant.now().plusSeconds(300)));
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));
        when(intentRepository.findFirstByOrderIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
                12, PaymentOption.FULL, PaymentStatus.PENDING)).thenReturn(Optional.of(active));

        PaymentQrResponse response = service.createForExistingOrder(12);

        assertEquals("PAY-EXISTING", response.getPaymentCode());
        verify(intentRepository, never()).save(any(PaymentIntent.class));
    }

    @Test
    void regeneratesQrWithoutCreatingAnotherOrder() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.UNPAID, 216_000.0);
        PaymentIntent existing = new PaymentIntent();
        existing.setId(3L);
        existing.setOrder(order);
        existing.setPaymentCode("PAY-OLD");
        existing.setPaymentOption(PaymentOption.FULL);
        existing.setStatus(PaymentStatus.PENDING);
        existing.setPaidAmount(BigDecimal.ZERO);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));
        when(intentRepository.findLockedByPaymentCode("PAY-OLD")).thenReturn(Optional.of(existing));
        when(intentRepository.findByIdempotencyKey("regen-key-001")).thenReturn(Optional.empty());

        PaymentQrResponse response = service.regenerate(12, "PAY-OLD", "regen-key-001");

        assertEquals(PaymentStatus.REPLACED, existing.getStatus());
        assertNotEquals("PAY-OLD", response.getPaymentCode());
        assertEquals("1234567890", response.getAccountNumber());
        verify(intentRepository).saveAndFlush(existing);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void neverDispatchesUnpaidTransferOrderManually() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.UNPAID, 216_000.0);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.confirmManualDispatch(12));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void cashierFlowCanDispatchCodOrderWithoutMarkingItPaid() {
        Order order = order(12, OrderPaymentOption.COD, PaymentStatus.UNPAID, 216_000.0);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        Order result = service.confirmManualDispatch(12);

        assertEquals(1, result.getStatus());
        assertEquals(false, result.getIsPaid());
        verify(inventoryReservationService).consume(12);
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
        verify(activityLogService).log("MANUAL_ORDER_CONFIRM", "Order", "12",
                "Xác nhận thủ công đơn COD/tại quán và chuyển xuống bếp");
    }

    @Test
    void adminCanConfirmTransferPaymentWithoutDispatchingToKitchen() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.UNPAID, 216_000.0);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        Order result = service.confirmTransferPayment(12, "cashier");

        assertEquals(true, result.getIsPaid());
        assertEquals(PaymentStatus.PAID, result.getPaymentStatus());
        assertEquals(new BigDecimal("216000"), result.getPaidAmount());
        assertEquals(BigDecimal.ZERO, result.getRemainingAmount());
        assertEquals("cashier", result.getPaymentConfirmedBy());
        verify(inventoryReservationService, never()).consume(12);
        verify(messagingTemplate).convertAndSend("/topic/orders", "ORDER_PAYMENT_CONFIRMED");
        verify(activityLogService).log("MANUAL_PAYMENT_CONFIRM", "Order", "12",
                "Xác nhận thanh toán chuyển khoản cho đơn hàng #12");
    }

    @Test
    void paidTransferOrderCanBeDispatchedByAnAuthorizedOperator() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.PAID, 216_000.0);
        order.setIsPaid(true);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        Order result = service.confirmManualDispatch(12);

        assertEquals(1, result.getStatus());
        verify(inventoryReservationService).consume(12);
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void legacyPaidTransferFlagCanBeNormalizedAndDispatched() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.UNPAID, 216_000.0);
        order.setIsPaid(true);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        Order result = service.confirmManualDispatch(12);

        assertEquals(1, result.getStatus());
        assertEquals(true, result.getIsPaid());
        assertEquals(PaymentStatus.PAID, result.getPaymentStatus());
        assertEquals(new BigDecimal("216000"), result.getPaidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getRemainingAmount()));
        verify(inventoryReservationService).consume(12);
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void repeatedDispatchIsRejectedWithoutDuplicatingKitchenNotification() {
        Order order = order(12, OrderPaymentOption.PAY_AT_RESTAURANT, PaymentStatus.UNPAID, 216_000.0);
        order.setStatus(1);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.confirmManualDispatch(12));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Đơn đã được chuyển xuống bếp trước đó", error.getReason());
        verify(inventoryReservationService, never()).consume(12);
        verify(messagingTemplate, never()).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void legacyOrderWithoutPaymentOptionReturnsASpecificConflictInsteadOfCrashing() {
        Order order = order(12, null, PaymentStatus.UNPAID, 216_000.0);
        order.setOrderCode("ORD-LEGACY-0000000012");
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.confirmManualDispatch(12));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("legacy thiếu hình thức thanh toán"));
        verify(messagingTemplate, never()).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void ledgerDispatchesOnlyAfterFullPayment() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.UNPAID, 216_000.0);
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        service.applyLedgerPayment(12, new BigDecimal("100000"), PaymentStatus.PARTIALLY_PAID);
        assertEquals(0, order.getStatus());
        assertEquals(false, order.getIsPaid());
        verify(messagingTemplate, never()).convertAndSend("/topic/kitchen", "NEW_ORDER");

        service.applyLedgerPayment(12, new BigDecimal("216000"), PaymentStatus.PAID);
        assertEquals(1, order.getStatus());
        assertEquals(true, order.getIsPaid());
        assertEquals(BigDecimal.ZERO, order.getRemainingAmount());
        verify(inventoryReservationService).consume(12);
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void orderLedgerAddsOnlyTheNewPaymentToReservationDeposit() {
        Order order = order(12, OrderPaymentOption.PREPAID_TRANSFER, PaymentStatus.PARTIALLY_PAID, 700_000.0);
        order.setDeposit(new BigDecimal("250000"));
        order.setPaidAmount(new BigDecimal("250000"));
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(order));

        service.applyLedgerPayment(12, new BigDecimal("200000"), PaymentStatus.PARTIALLY_PAID);

        assertEquals(new BigDecimal("450000"), order.getPaidAmount());
        assertEquals(new BigDecimal("250000"), order.getRemainingAmount());
        assertEquals(PaymentStatus.PARTIALLY_PAID, order.getPaymentStatus());
    }

    private Order order(int id, OrderPaymentOption option, PaymentStatus status, double total) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(0);
        order.setIsPaid(false);
        order.setPaymentOption(option);
        order.setPaymentStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(total));
        return order;
    }

    private static PaymentProperties properties() {
        PaymentProperties properties = new PaymentProperties();
        properties.setBankCode("MB");
        properties.setBankBin("970422");
        properties.setAccountNumber("1234567890");
        properties.setAccountHolder("Hoàng Nguyễn Minh Tâm");
        properties.setQrProvider("VIETQR");
        properties.setQrExpirationMinutes(15);
        return properties;
    }
}
