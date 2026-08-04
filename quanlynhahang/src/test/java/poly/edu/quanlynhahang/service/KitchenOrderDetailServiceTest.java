package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;

class KitchenOrderDetailServiceTest {
    private final OrderDetailRepository orderDetailRepository = mock(OrderDetailRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final KitchenOrderDetailService service = new KitchenOrderDetailService(
            orderDetailRepository, activityLogService, messagingTemplate);

    @Test
    void startsThenCompletesPendingDishAndEmitsEvents() {
        OrderDetail detail = pendingDetail();
        when(orderDetailRepository.findById(7)).thenReturn(Optional.of(detail));
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetail started = service.start(7);
        OrderDetail completed = service.complete(7);

        assertNotNull(started.getStartedAt());
        assertEquals(1, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "DISH_STARTED");
        verify(messagingTemplate).convertAndSend("/topic/waiter", "DISH_READY");
    }

    @Test
    void refusesCompletionBeforeDishWasStarted() {
        when(orderDetailRepository.findById(7)).thenReturn(Optional.of(pendingDetail()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.complete(7));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void cancelsPendingDishWithReason() {
        OrderDetail detail = pendingDetail();
        when(orderDetailRepository.findById(7)).thenReturn(Optional.of(detail));
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetail cancelled = service.cancel(7, "Hết nguyên liệu");

        assertEquals(3, cancelled.getStatus());
        assertEquals("Hết nguyên liệu", cancelled.getCancelReason());
        assertNotNull(cancelled.getCancelledAt());
    }

    private OrderDetail pendingDetail() {
        OrderDetail detail = new OrderDetail();
        detail.setId(7);
        detail.setStatus(0);
        return detail;
    }
}
