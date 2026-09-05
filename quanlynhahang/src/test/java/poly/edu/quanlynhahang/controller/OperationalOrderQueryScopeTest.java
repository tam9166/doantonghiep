package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.service.InventoryAlertService;
import poly.edu.quanlynhahang.service.OrderServiceDateGuardService;

class OperationalOrderQueryScopeTest {
    @Test
    void revenueAnalyticsIncludesPaidAndServedOrdersButCountsOnlyCompletedInvoices() {
        Order served = new Order();
        served.setStatus(OrderStatus.SERVED.code());
        served.setTotalAmount(new BigDecimal("120.00"));
        OrderDetail servedDish = new OrderDetail();
        servedDish.setQuantity(2);
        served.setOrderDetails(List.of(servedDish));

        Order paidPending = new Order();
        paidPending.setStatus(OrderStatus.PENDING.code());
        paidPending.setIsPaid(true);
        paidPending.setPaymentStatus(PaymentStatus.PAID);
        paidPending.setTotalAmount(new BigDecimal("80.00"));
        OrderDetail paidDish = new OrderDetail();
        paidDish.setQuantity(1);
        paidPending.setOrderDetails(List.of(paidDish));

        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findOrdersForRevenueAnalytics()).thenReturn(List.of(served, paidPending));
        when(repository.countByStatus(OrderStatus.PENDING.code())).thenReturn(1L);
        AdminOrderController controller = new AdminOrderController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> stats = (java.util.Map<String, Object>) controller.getRevenueAnalytics().getBody();

        assertEquals(new BigDecimal("200.00"), stats.get("totalRevenue"));
        assertEquals(1L, stats.get("completedOrdersCount"));
        assertEquals(2, stats.get("totalItemsSold"));
        assertEquals(1L, stats.get("pendingOrdersCount"));
        verify(repository).findOrdersForRevenueAnalytics();
    }

    @Test
    void kitchenBoardQueriesActiveAndTodayCompletedDishLifecycles() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findKitchenBoardOrdersWithDetails(any(), any(), any(), any(), any(Date.class)))
                .thenReturn(List.of());
        AdminOrderController controller = new AdminOrderController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        assertEquals(List.of(), controller.getKitchenBoard().getBody());

        verify(repository).findKitchenBoardOrdersWithDetails(
                eq(OrderStatus.CANCELLED.code()), eq(PaymentStatus.REFUNDED),
                eq(List.of(0)), eq(List.of(1, 2)),
                any(Date.class));
        verify(repository, never()).findAllWithDetails();
    }

    @Test
    void kitchenBoardExcludesPreorderBeforePreparationTime() {
        Order futurePreorder = new Order();
        futurePreorder.setId(77);
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findKitchenBoardOrdersWithDetails(any(), any(), any(), any(), any(Date.class)))
                .thenReturn(List.of(futurePreorder));
        OrderServiceDateGuardService timing = mock(OrderServiceDateGuardService.class);
        when(timing.isPreparationReached(futurePreorder)).thenReturn(false);
        AdminOrderController controller = new AdminOrderController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);
        ReflectionTestUtils.setField(controller, "serviceDateGuard", timing);

        assertEquals(List.of(), controller.getKitchenBoard().getBody());
        verify(timing).isPreparationReached(futurePreorder);
        verify(timing, never()).resolveTiming(futurePreorder);
    }

    @Test
    void waiterQueueUsesDishLifecycleInsteadOfParentOrderState() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findWaiterOperationalOrdersWithDetails(
                OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED, List.of(0, 1)))
                .thenReturn(List.of());
        poly.edu.quanlynhahang.controller.WaiterController controller = new poly.edu.quanlynhahang.controller.WaiterController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        assertEquals(List.of(), controller.getReadyOrders().getBody());
        verify(repository).findWaiterOperationalOrdersWithDetails(
                OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED, List.of(0, 1));
    }

    @Test
    void waiterQueueReturnsCookingDishEvenWhenParentIsNotReady() {
        Order order = new Order();
        order.setId(41);
        order.setStatus(OrderStatus.IN_PREPARATION.code());
        OrderDetail cooking = new OrderDetail();
        cooking.setId(401);
        cooking.setStatus(0);
        cooking.setOrder(order);
        order.setOrderDetails(List.of(cooking));

        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findWaiterOperationalOrdersWithDetails(
                OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED, List.of(0, 1)))
                .thenReturn(List.of(order));
        poly.edu.quanlynhahang.controller.WaiterController controller = new poly.edu.quanlynhahang.controller.WaiterController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        @SuppressWarnings("unchecked")
        List<poly.edu.quanlynhahang.dto.OrderResponse> body =
                (List<poly.edu.quanlynhahang.dto.OrderResponse>) controller.getReadyOrders().getBody();

        assertEquals(1, body.size());
        assertEquals(OrderStatus.IN_PREPARATION.code(), body.get(0).status());
        assertEquals(0, body.get(0).orderDetails().get(0).status());
    }

    @Test
    void waiterQueueExcludesPreorderBeforePreparationTime() {
        Order futurePreorder = new Order();
        futurePreorder.setId(78);
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findWaiterOperationalOrdersWithDetails(
                OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED, List.of(0, 1)))
                .thenReturn(List.of(futurePreorder));
        OrderServiceDateGuardService timing = mock(OrderServiceDateGuardService.class);
        when(timing.isPreparationReached(futurePreorder)).thenReturn(false);
        WaiterController controller = new WaiterController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);
        ReflectionTestUtils.setField(controller, "serviceDateGuard", timing);

        assertEquals(List.of(), controller.getReadyOrders().getBody());
        verify(timing).isPreparationReached(futurePreorder);
        verify(timing, never()).resolveTiming(futurePreorder);
    }

    @Test
    void waiterQueuePreservesCookingAndReadyDetailsForPartiallyReadyParent() {
        Order order = new Order();
        order.setId(42);
        order.setStatus(OrderStatus.PARTIALLY_READY.code());
        OrderDetail cooking = new OrderDetail();
        cooking.setId(421);
        cooking.setStatus(0);
        cooking.setOrder(order);
        OrderDetail ready = new OrderDetail();
        ready.setId(422);
        ready.setStatus(1);
        ready.setOrder(order);
        order.setOrderDetails(List.of(cooking, ready));

        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findWaiterOperationalOrdersWithDetails(
                OrderStatus.CANCELLED.code(), PaymentStatus.REFUNDED, List.of(0, 1)))
                .thenReturn(List.of(order));
        WaiterController controller = new WaiterController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        @SuppressWarnings("unchecked")
        List<poly.edu.quanlynhahang.dto.OrderResponse> body =
                (List<poly.edu.quanlynhahang.dto.OrderResponse>) controller.getReadyOrders().getBody();

        assertEquals(OrderStatus.PARTIALLY_READY.code(), body.get(0).status());
        assertEquals(List.of(0, 1), body.get(0).orderDetails().stream()
                .map(poly.edu.quanlynhahang.dto.OrderDetailResponse::status).toList());
    }

    @Test
    void adminOrderListCapsItsDefaultArrayContractToFiveHundredRows() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findRecentOrderIds(PageRequest.of(0, 500))).thenReturn(List.of());
        AdminOrderController controller = new AdminOrderController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        assertEquals(List.of(), controller.getAllOrders(null, 10, 9999).getBody());

        verify(repository).findRecentOrderIds(PageRequest.of(0, 500));
        verify(repository, never()).findAllWithDetailsByIdIn(any());
    }

    @Test
    void adminOrderListSupportsTenRowPageContractWhenPageParameterIsPresent() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findRecentOrderIds(PageRequest.of(1, 10))).thenReturn(List.of());
        when(repository.count()).thenReturn(23L);
        AdminOrderController controller = new AdminOrderController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        @SuppressWarnings("unchecked")
        Page<?> page = (Page<?>) controller.getAllOrders(1, 10, 200).getBody();

        assertEquals(10, page.getSize());
        assertEquals(1, page.getNumber());
        assertEquals(23, page.getTotalElements());
        verify(repository).findRecentOrderIds(PageRequest.of(1, 10));
        verify(repository).count();
    }

    @Test
    void purchaseSuggestionsUseTheSharedInventoryAnalysis() {
        InventoryAlertService inventoryAlertService = mock(InventoryAlertService.class);
        InventoryAlertService.Analysis analysis = new InventoryAlertService.Analysis(
                List.of(), 0, 0, 0, 0, 0, 0, 0, 0, 0, java.math.BigDecimal.ZERO);
        when(inventoryAlertService.analyze(3)).thenReturn(analysis);
        PurchaseSuggestionController controller = new PurchaseSuggestionController(
                mock(poly.edu.quanlynhahang.repository.IngredientRepository.class),
                mock(poly.edu.quanlynhahang.repository.IngredientBatchRepository.class),
                inventoryAlertService,
                mock(poly.edu.quanlynhahang.service.InventoryImportService.class));

        assertEquals(analysis, controller.getSuggestions().getBody());

        verify(inventoryAlertService).analyze(3);
    }

    @Test
    void inventoryDashboardStatsUseTheSameExpiredBatchAnalysis() {
        InventoryAlertService inventoryAlertService = mock(InventoryAlertService.class);
        InventoryAlertService.Analysis analysis = new InventoryAlertService.Analysis(
                List.of(), 8, 3, 1, 24, 2, 0, 4, 0, 0, java.math.BigDecimal.ZERO);
        when(inventoryAlertService.analyze(3)).thenReturn(analysis);
        IngredientController controller = new IngredientController();
        ReflectionTestUtils.setField(controller, "inventoryAlertService", inventoryAlertService);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> stats = (java.util.Map<String, Object>) controller.getStats().getBody();

        assertEquals(24L, stats.get("expiredBatchesCount"));
        assertEquals(2L, stats.get("expiringBatchesCount"));
        assertEquals(3L, stats.get("lowStock"));
        verify(inventoryAlertService).analyze(3);
    }
}
