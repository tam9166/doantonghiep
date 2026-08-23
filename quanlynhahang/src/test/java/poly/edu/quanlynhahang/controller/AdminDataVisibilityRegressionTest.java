package poly.edu.quanlynhahang.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.test.util.ReflectionTestUtils;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Date;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AdminDataVisibilityRegressionTest {

    @Test
    void staffListUsesRepositoryQueryThatLoadsAuthorities() {
        Account account = new Account();
        account.setUsername("admin");
        account.setEnabled(true);
        Role role = new Role();
        role.setName("ADMIN");
        Authority authority = new Authority();
        authority.setRole(role);
        authority.setAccount(account);
        account.setAuthorities(List.of(authority));

        AccountRepository repository = mock(AccountRepository.class);
        when(repository.findAllWithAuthorities()).thenReturn(List.of(account));
        AdminAccountController controller = new AdminAccountController();
        ReflectionTestUtils.setField(controller, "accountRepository", repository);

        List<?> response = (List<?>) controller.getAllStaff().getBody();

        assertNotNull(response);
        assertEquals(1, response.size());
        verify(repository).findAllWithAuthorities();
    }

    @Test
    void popularItemEndpointsLoadOrdersWithDetails() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        when(orderRepository.findByStatusSinceWithDetails(eq(4), any(Date.class))).thenReturn(List.of());
        PopularItemsController controller = new PopularItemsController();
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "recipeRepository", recipeRepository);

        assertEquals(List.of(), controller.getTopProducts("month").getBody());
        assertEquals(List.of(), controller.getTopIngredients("month").getBody());
        verify(orderRepository, org.mockito.Mockito.times(2))
                .findByStatusSinceWithDetails(eq(4), any(Date.class));
        verify(orderRepository, org.mockito.Mockito.never()).findAllWithDetails();
        verify(recipeRepository, org.mockito.Mockito.never()).findAll();
    }

    @Test
    void popularIngredientsSortsDecimalConsumptionWithoutClassCastFailure() {
        Product product = new Product();
        product.setId(10);
        product.setName("Món thử");
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(3);
        Order order = new Order();
        order.setOrderDetails(List.of(detail));

        Ingredient ingredient = new Ingredient();
        ingredient.setName("Gạo");
        ingredient.setUnit("kg");
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(new BigDecimal("0.1250"));

        OrderRepository orderRepository = mock(OrderRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        when(orderRepository.findByStatusSinceWithDetails(eq(4), any(Date.class)))
                .thenReturn(List.of(order));
        when(recipeRepository.findByProductIdsWithIngredient(List.of(10)))
                .thenReturn(List.of(recipe));
        PopularItemsController controller = new PopularItemsController();
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "recipeRepository", recipeRepository);

        List<?> result = (List<?>) controller.getTopIngredients("week").getBody();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("0.3750"),
                ((java.util.Map<?, ?>) result.getFirst()).get("totalConsumed"));
    }

    @Test
    void reservationListEntityGraphOnlyReferencesMappedRelationships() throws Exception {
        Method method = ReservationRepository.class.getMethod("findAllByOrderByCreatedAtDesc");
        EntityGraph graph = method.getAnnotation(EntityGraph.class);

        assertNotNull(graph);
        assertArrayEquals(new String[]{
                "area", "table", "tableAssignments", "tableAssignments.table"
        }, graph.attributePaths());
    }
}
