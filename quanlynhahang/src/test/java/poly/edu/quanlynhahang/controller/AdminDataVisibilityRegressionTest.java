package poly.edu.quanlynhahang.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.test.util.ReflectionTestUtils;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(orderRepository.findAllWithDetails()).thenReturn(List.of());
        when(recipeRepository.findAll()).thenReturn(List.of());
        PopularItemsController controller = new PopularItemsController();
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "recipeRepository", recipeRepository);

        assertEquals(List.of(), controller.getTopProducts("month").getBody());
        assertEquals(List.of(), controller.getTopIngredients("month").getBody());
        verify(orderRepository, org.mockito.Mockito.times(2)).findAllWithDetails();
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
