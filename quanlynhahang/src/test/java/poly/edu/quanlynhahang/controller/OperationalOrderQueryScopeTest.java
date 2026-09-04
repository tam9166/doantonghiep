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

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.service.InventoryAlertService;

class OperationalOrderQueryScopeTest {
    @Test
    void kitchenBoardQueriesOnlyActiveAndTodayCompletedStatuses() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findKitchenBoardOrdersWithDetails(any(), any(), any(Date.class))).thenReturn(List.of());
        AdminOrderController controller = new AdminOrderController();
        ReflectionTestUtils.setField(controller, "orderRepository", repository);

        assertEquals(List.of(), controller.getKitchenBoard().getBody());

        verify(repository).findKitchenBoardOrdersWithDetails(
                eq(List.of(OrderStatus.IN_PREPARATION.code(), OrderStatus.PARTIALLY_READY.code())),
                eq(List.of(OrderStatus.READY.code(), OrderStatus.COMPLETED.code(), OrderStatus.SERVED.code())),
                any(Date.class));
        verify(repository, never()).findAllWithDetails();
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
