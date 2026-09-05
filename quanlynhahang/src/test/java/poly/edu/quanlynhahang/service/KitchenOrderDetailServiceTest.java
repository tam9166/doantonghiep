package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.RefundTransaction;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;

class KitchenOrderDetailServiceTest {
    private final OrderDetailRepository orderDetailRepository = mock(OrderDetailRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final RefundTransactionRepository refundRepository = mock(RefundTransactionRepository.class);
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final OrderServiceDateGuardService serviceDateGuard = mock(OrderServiceDateGuardService.class);
    private final KitchenOrderDetailService service = new KitchenOrderDetailService(
            orderDetailRepository, activityLogService, messagingTemplate, orderRepository, recipeRepository,
            refundRepository, inventoryReservationService, new OrderStateMachineService(), serviceDateGuard);

    @Test
    void startsThenCompletesPendingDishAndEmitsEvents() {
        OrderDetail detail = pendingDetail();
        Order order = new Order();
        order.setId(12);
        order.setStatus(0);
        order.setOrderDetails(List.of(detail));
        detail.setOrder(order);
        when(orderDetailRepository.findById(7)).thenReturn(Optional.of(detail));
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetail started = service.start(7);
        OrderDetail completed = service.complete(7);

        assertNotNull(started.getStartedAt());
        assertEquals(2, order.getStatus());
        assertEquals(1, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        verify(orderRepository, org.mockito.Mockito.times(2)).save(order);
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
    void publishesDishReadyOnlyAfterTheTransactionCommits() {
        OrderDetail detail = pendingDetail();
        detail.setStartedAt(new java.util.Date());
        Order order = new Order();
        order.setId(12);
        order.setStatus(1);
        order.setOrderDetails(List.of(detail));
        detail.setOrder(order);
        when(orderDetailRepository.findById(7)).thenReturn(Optional.of(detail));
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.complete(7);
            verify(messagingTemplate, never()).convertAndSend("/topic/waiter", "DISH_READY");

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCommit());

            verify(messagingTemplate).convertAndSend("/topic/waiter", "DISH_READY");
            verify(messagingTemplate).convertAndSend("/topic/kitchen", "DISH_READY");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void waiterServesOnlyACompletedDishAndAdvancesTheParentOrder() {
        Order order = new Order();
        order.setId(12);
        order.setStatus(2);
        OrderDetail detail = pendingDetail();
        detail.setStatus(1);
        detail.setCompletedAt(new java.util.Date());
        detail.setOrder(order);
        order.setOrderDetails(List.of(detail));
        when(orderDetailRepository.findLockedWithOrderAndProductById(7)).thenReturn(Optional.of(detail));
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetail served = service.serve(7);

        assertEquals(2, served.getStatus());
        assertEquals(7, order.getStatus());
        verify(orderRepository).save(order);
        verify(messagingTemplate).convertAndSend("/topic/waiter", "DISH_SERVED");
    }

    @Test
    void waiterCanServeLastReadyDishAfterInvoiceWasAlreadyCompleted() {
        Order order = new Order();
        order.setId(12);
        order.setStatus(poly.edu.quanlynhahang.entity.OrderStatus.COMPLETED.code());
        OrderDetail detail = pendingDetail();
        detail.setStatus(1);
        detail.setCompletedAt(new java.util.Date());
        detail.setOrder(order);
        order.setOrderDetails(List.of(detail));
        when(orderDetailRepository.findLockedWithOrderAndProductById(7)).thenReturn(Optional.of(detail));
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetail served = service.serve(7);

        assertEquals(2, served.getStatus());
        assertEquals(poly.edu.quanlynhahang.entity.OrderStatus.COMPLETED.code(), order.getStatus());
        verify(orderRepository, never()).save(order);
        verify(messagingTemplate).convertAndSend("/topic/waiter", "DISH_SERVED");
    }

    @Test
    void waiterRejectsAlreadyProcessedDishBeforeAnyMutation() {
        Order order = new Order();
        order.setId(12);
        order.setStatus(poly.edu.quanlynhahang.entity.OrderStatus.COMPLETED.code());
        OrderDetail detail = pendingDetail();
        detail.setStatus(2);
        detail.setCompletedAt(new java.util.Date());
        detail.setOrder(order);
        order.setOrderDetails(List.of(detail));
        when(orderDetailRepository.findLockedWithOrderAndProductById(7)).thenReturn(Optional.of(detail));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.serve(7));

        assertEquals("Món này đã hoàn thành hoặc đã được xử lý.", error.getReason());
        assertEquals(2, detail.getStatus());
        verify(orderDetailRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void futureServiceDateBlocksStartCompleteAndServeBeforeAnyMutation() {
        OrderDetail detail = pendingDetail();
        Order order = new Order();
        order.setId(12);
        detail.setOrder(order);
        when(orderDetailRepository.findById(7)).thenReturn(Optional.of(detail));
        org.mockito.Mockito.doThrow(new ResponseStatusException(
                HttpStatus.CONFLICT, OrderServiceDateGuardService.FUTURE_SERVICE_MESSAGE))
                .when(serviceDateGuard).assertPreparationReached(order);

        ResponseStatusException startError = assertThrows(ResponseStatusException.class, () -> service.start(7));
        assertEquals(OrderServiceDateGuardService.FUTURE_SERVICE_MESSAGE, startError.getReason());
        verify(orderDetailRepository, never()).save(any());

        detail.setStartedAt(new java.util.Date());
        ResponseStatusException completeError = assertThrows(ResponseStatusException.class, () -> service.complete(7));
        assertEquals(OrderServiceDateGuardService.FUTURE_SERVICE_MESSAGE, completeError.getReason());

        detail.setStatus(1);
        detail.setCompletedAt(new java.util.Date());
        when(orderDetailRepository.findLockedWithOrderAndProductById(7)).thenReturn(Optional.of(detail));
        ResponseStatusException serveError = assertThrows(ResponseStatusException.class, () -> service.serve(7));
        assertEquals(OrderServiceDateGuardService.FUTURE_SERVICE_MESSAGE, serveError.getReason());
    }

    @Test
    void cancelsPendingDishWithReason() {
        OrderDetail detail = cancellableDetail(BigDecimal.ZERO);
        when(orderDetailRepository.findLockedWithOrderAndProductById(7)).thenReturn(Optional.of(detail));
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(detail.getOrder()));
        when(recipeRepository.findByProduct(detail.getProduct())).thenReturn(List.of(recipe(detail.getProduct())));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(12)).thenReturn(List.of());
        when(orderDetailRepository.countByOrderIdAndStatusNot(12, 3)).thenReturn(1L);
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetail cancelled = service.cancel(7, "Hết nguyên liệu", "chef01");

        assertEquals(3, cancelled.getStatus());
        assertEquals("Hết nguyên liệu", cancelled.getCancelReason());
        assertEquals("chef01", cancelled.getCancelledBy());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals(new BigDecimal("0.00"), detail.getOrder().getTotalAmount());
        verify(inventoryReservationService).adjustForCancelledItem(
                12, java.util.Map.of(10L, new BigDecimal("2.0")), true);
        verify(refundRepository, never()).save(any());
    }

    @Test
    void prepaidDishCancellationCreatesPendingRefundForExcessPaidAmount() {
        OrderDetail detail = cancellableDetail(new BigDecimal("108"));
        when(orderDetailRepository.findLockedWithOrderAndProductById(7)).thenReturn(Optional.of(detail));
        when(orderRepository.findLockedById(12)).thenReturn(Optional.of(detail.getOrder()));
        when(recipeRepository.findByProduct(detail.getProduct())).thenReturn(List.of(recipe(detail.getProduct())));
        when(refundRepository.findByOrderIdOrderByCreatedAtDesc(12)).thenReturn(List.of());
        when(orderDetailRepository.countByOrderIdAndStatusNot(12, 3)).thenReturn(1L);
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancel(7, "Không thể phục vụ", "chef01");

        org.mockito.ArgumentCaptor<RefundTransaction> refund = org.mockito.ArgumentCaptor.forClass(RefundTransaction.class);
        verify(refundRepository).save(refund.capture());
        assertEquals(new BigDecimal("108"), refund.getValue().getAmount());
        assertEquals(RefundTransaction.RefundStatus.PENDING, refund.getValue().getStatus());
        assertEquals(PaymentStatus.REFUND_PENDING, detail.getOrder().getPaymentStatus());
    }

    private OrderDetail pendingDetail() {
        OrderDetail detail = new OrderDetail();
        detail.setId(7);
        detail.setStatus(0);
        return detail;
    }

    private OrderDetail cancellableDetail(BigDecimal paid) {
        Product product = new Product();
        product.setId(1);
        Order order = new Order();
        order.setId(12);
        order.setStatus(1);
        order.setSubTotal(new BigDecimal("100.00"));
        order.setTaxAmount(new BigDecimal("8.00"));
        order.setTotalAmount(new BigDecimal("108.00"));
        order.setPaidAmount(paid);
        order.setRemainingAmount(new BigDecimal("108"));
        order.setPaymentStatus(paid.signum() > 0 ? PaymentStatus.PAID : PaymentStatus.UNPAID);
        OrderDetail detail = pendingDetail();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setQuantity(1);
        detail.setPrice(new BigDecimal("100.00"));
        detail.setTaxAmount(new BigDecimal("8.00"));
        return detail;
    }

    private Recipe recipe(Product product) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(10L);
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(new BigDecimal("2.0"));
        return recipe;
    }
}
