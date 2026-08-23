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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

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

        assertEquals(List.of(), controller.getAllOrders(9999).getBody());

        verify(repository).findRecentOrderIds(PageRequest.of(0, 500));
        verify(repository, never()).findAllWithDetailsByIdIn(any());
    }

    @Test
    void purchaseSuggestionsQueryOnlyRecentCompletedOrders() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        IngredientRepository ingredientRepository = mock(IngredientRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        when(ingredientRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findByStatusSinceWithDetails(eq(OrderStatus.COMPLETED.code()), any(Date.class)))
                .thenReturn(List.of());
        PurchaseSuggestionController controller = new PurchaseSuggestionController();
        ReflectionTestUtils.setField(controller, "ingredientRepository", ingredientRepository);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "recipeRepository", recipeRepository);

        controller.getSuggestions();

        verify(orderRepository).findByStatusSinceWithDetails(
                eq(OrderStatus.COMPLETED.code()), any(Date.class));
        verify(orderRepository, never()).findAll();
        verify(recipeRepository, never()).findAll();
    }
}
