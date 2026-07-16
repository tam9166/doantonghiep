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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPaymentServiceTest {
    private final PaymentIntentRepository intentRepository = mock(PaymentIntentRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
    private final PaymentProperties properties = properties();
    private final OrderPaymentService service = new OrderPaymentService(
            intentRepository, orderRepository, properties, activityLogService, messagingTemplate, tableRepository);

    @BeforeEach
    void setUp() {
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
        assertEquals("919112006789", response.getAccountNumber());
        assertTrue(response.getTransferContent().startsWith("TT DH12 "));
        assertTrue(response.getQrUrl().contains("amount=216000"));
        verify(intentRepository).save(any(PaymentIntent.class));
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
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
        verify(activityLogService).log("MANUAL_ORDER_CONFIRM", "Order", "12",
                "Xác nhận thủ công đơn COD/tại quán và chuyển xuống bếp");
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
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    private Order order(int id, OrderPaymentOption option, PaymentStatus status, double total) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(0);
        order.setIsPaid(false);
        order.setPaymentOption(option);
        order.setPaymentStatus(status);
        order.setTotalAmount(total);
        return order;
    }

    private static PaymentProperties properties() {
        PaymentProperties properties = new PaymentProperties();
        properties.setBankCode("MB");
        properties.setBankBin("970422");
        properties.setAccountNumber("919112006789");
        properties.setAccountHolder("HOANG NGUYEN MINH TAM");
        properties.setQrProvider("VIETQR");
        properties.setQrExpirationMinutes(15);
        return properties;
    }
}
