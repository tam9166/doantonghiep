package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.repository.OrderVoucherUsageRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;

class OrderFinancialMutationGuardServiceTest {
    private final PaymentIntentRepository intents = mock(PaymentIntentRepository.class);
    private final PaymentTransactionRepository ledger = mock(PaymentTransactionRepository.class);
    private final RefundTransactionRepository refunds = mock(RefundTransactionRepository.class);
    private final OrderVoucherUsageRepository vouchers = mock(OrderVoucherUsageRepository.class);
    private final InventoryReservationRepository inventory = mock(InventoryReservationRepository.class);
    private final OrderFinancialMutationGuardService service = new OrderFinancialMutationGuardService(
            intents, ledger, refunds, vouchers, inventory);

    @Test
    void allowsAnUnpaidOrderWithoutFinancialAllocation() {
        assertDoesNotThrow(() -> service.requireSafeForTableComposition(order()));
    }

    @Test
    void blocksAfterAnyPaidAmount() {
        Order order = order();
        order.setPaidAmount(BigDecimal.ONE);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireSafeForTableComposition(order));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void blocksPendingPaymentIntentOrVoucherAllocation() {
        Order order = order();
        when(intents.existsByOrderId(12)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.requireSafeForTableComposition(order)).getStatusCode());

        when(intents.existsByOrderId(12)).thenReturn(false);
        when(vouchers.existsByOrderId(12)).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.requireSafeForTableComposition(order)).getStatusCode());
    }

    @Test
    void blocksAnInventoryHoldThatCannotYetBeReallocated() {
        Order order = order();
        when(inventory.existsByOrderIdAndStatus(12, InventoryReservationStatus.RESERVED)).thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.requireSafeForTableComposition(order)).getStatusCode());
    }

    private Order order() {
        Order order = new Order();
        order.setId(12);
        order.setIsPaid(false);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        return order;
    }
}
