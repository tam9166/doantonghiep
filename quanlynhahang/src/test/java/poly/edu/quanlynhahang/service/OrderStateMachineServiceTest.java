package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderStatus;

class OrderStateMachineServiceTest {
    private final OrderStateMachineService service = new OrderStateMachineService();

    @Test
    void rejectsUnknownAndSkippedOperationalStates() {
        Order order = order(OrderStatus.PENDING, detail(0));

        ResponseStatusException unknown = assertThrows(ResponseStatusException.class,
                () -> service.transition(order, 999));
        ResponseStatusException skipped = assertThrows(ResponseStatusException.class,
                () -> service.transition(order, OrderStatus.SERVED));

        assertEquals(HttpStatus.BAD_REQUEST, unknown.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, skipped.getStatusCode());
        assertEquals(OrderStatus.PENDING.code(), order.getStatus());
    }

    @Test
    void derivesReadyStateOnlyAfterEveryActiveDishIsReady() {
        OrderDetail first = detail(1);
        OrderDetail second = detail(0);
        Order order = order(OrderStatus.IN_PREPARATION, first, second);

        service.refreshFromDishStates(order);
        assertEquals(OrderStatus.PARTIALLY_READY.code(), order.getStatus());

        second.setStatus(1);
        service.refreshFromDishStates(order);
        assertEquals(OrderStatus.READY.code(), order.getStatus());
    }

    @Test
    void waiterCannotServeUntilEveryActiveDishWasServed() {
        Order order = order(OrderStatus.READY, detail(2), detail(1));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transition(order, OrderStatus.SERVED));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(OrderStatus.READY.code(), order.getStatus());
    }

    @Test
    void cancelledOrderCannotBeReopened() {
        Order order = order(OrderStatus.CANCELLED, detail(3));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transition(order, OrderStatus.PENDING));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(OrderStatus.CANCELLED.code(), order.getStatus());
    }

    private Order order(OrderStatus status, OrderDetail... details) {
        Order order = new Order();
        order.setStatus(status.code());
        order.setOrderDetails(List.of(details));
        for (OrderDetail detail : details) detail.setOrder(order);
        return order;
    }

    private OrderDetail detail(int status) {
        OrderDetail detail = new OrderDetail();
        detail.setStatus(status);
        return detail;
    }
}
