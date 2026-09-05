package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;

class TableReleaseGuardServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentIntentRepository intents = mock(PaymentIntentRepository.class);
    private final InventoryReservationRepository inventoryReservations = mock(InventoryReservationRepository.class);
    private final TableReleaseGuardService service = new TableReleaseGuardService(
            orders, intents, inventoryReservations, new OrderStateMachineService());

    @Test
    void blocksReleaseWhileInventoryIsStillReserved() {
        Order order = order(true, BigDecimal.ZERO, 2);
        when(orders.findOrdersByTableIdWithDetails(3)).thenReturn(List.of(order));
        when(inventoryReservations.existsByOrderIdAndStatus(8, InventoryReservationStatus.RESERVED))
                .thenReturn(true);

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.prepareForRelease(3)).getStatusCode());
    }

    @Test
    void blocksReleaseWhileAnOrderIsUnpaid() {
        Order order = order(false, BigDecimal.TEN, 2);
        when(orders.findOrdersByTableIdWithDetails(3)).thenReturn(List.of(order));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.prepareForRelease(3));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void ignoresStaleReservationPreorderThatRetainsATableId() {
        // Regression: a cancelled reservation on B01 was blocked by order 22433.
        // That row is a TAKEAWAY preorder linked to a different reservation, not a
        // dine-in invoice, even though it still has table_id = B01 and is unpaid.
        Order stalePreorder = order(false, new BigDecimal("64000"), 1);
        stalePreorder.setOrderType(OrderType.TAKEAWAY);
        when(orders.findOrdersByTableIdWithDetails(3)).thenReturn(List.of(stalePreorder));

        assertDoesNotThrow(() -> service.prepareForRelease(3));
    }

    @Test
    void blocksReleaseWhileAPaymentIntentIsPending() {
        poly.edu.quanlynhahang.entity.PaymentIntent intent = new poly.edu.quanlynhahang.entity.PaymentIntent();
        intent.setStatus(PaymentStatus.PENDING);
        when(intents.findLockedByOrderTableId(3)).thenReturn(List.of(intent));

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.prepareForRelease(3)).getStatusCode());
    }

    @Test
    void blocksReleaseWhileAPaymentIsOverpaid() {
        poly.edu.quanlynhahang.entity.PaymentIntent intent = new poly.edu.quanlynhahang.entity.PaymentIntent();
        intent.setStatus(PaymentStatus.OVERPAID);
        when(intents.findLockedByOrderTableId(3)).thenReturn(List.of(intent));

        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> service.prepareForRelease(3)).getStatusCode());
    }

    @Test
    void completesAFullyPaidOrderOnlyAfterAllDishesWereServed() {
        Order order = order(true, BigDecimal.ZERO, 2);
        when(orders.findOrdersByTableIdWithDetails(3)).thenReturn(List.of(order));

        service.prepareForRelease(3);

        assertEquals(4, order.getStatus());
        verify(orders).save(order);
    }

    @Test
    void selectedUnpaidInvoiceDoesNotBlockItsOwnPaymentReleaseCheck() {
        Order selected = order(false, new BigDecimal("229000"), 2);
        when(orders.findOrdersByTableIdWithDetails(3)).thenReturn(List.of(selected));

        assertDoesNotThrow(() -> service.prepareForRelease(3, selected.getId()));

        org.mockito.Mockito.verify(orders, org.mockito.Mockito.never()).save(selected);
    }

    @Test
    void aDifferentOpenInvoiceStillBlocksPaymentReleaseCheck() {
        Order selected = order(false, new BigDecimal("229000"), 2);
        Order other = order(false, new BigDecimal("50000"), 2);
        other.setId(9);
        when(orders.findOrdersByTableIdWithDetails(3)).thenReturn(List.of(selected, other));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.prepareForRelease(3, selected.getId()));

        assertEquals("Còn hóa đơn khác chưa thanh toán trên bàn này", error.getReason());
    }

    private Order order(boolean paid, BigDecimal remaining, int detailStatus) {
        Order order = new Order();
        order.setId(8);
        order.setStatus(7);
        order.setOrderType(OrderType.DINE_IN);
        order.setIsPaid(paid);
        order.setRemainingAmount(remaining);
        order.setPaymentStatus(paid ? PaymentStatus.PAID : PaymentStatus.UNPAID);
        OrderDetail detail = new OrderDetail();
        detail.setStatus(detailStatus);
        detail.setOrder(order);
        order.setOrderDetails(List.of(detail));
        return order;
    }
}
