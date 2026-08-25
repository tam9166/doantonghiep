package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.RecipeRepository;

@ExtendWith(MockitoExtension.class)
class MenuEconomicsServiceTest {
    @Mock RecipeRepository recipeRepository;
    @Mock MenuAvailabilityService menuAvailabilityService;
    @InjectMocks MenuEconomicsService service;

    @Test
    void computesRecipeCostAndAvailableServingsFromCanonicalInventoryService() {
        Product product = product("100000");
        Ingredient beef = ingredient("200000");
        Ingredient noodles = ingredient("40000");
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(
                recipe(product, beef, "0.2"), recipe(product, noodles, "0.15")));
        when(menuAvailabilityService.potentialAvailableQuantity(product)).thenReturn(20);

        MenuEconomicsService.Assessment result = service.assess(product);

        assertEquals(new BigDecimal("46000.00"), result.costPrice());
        assertEquals(20, result.availableServings());
        assertEquals(true, result.hasRecipe());
    }

    @Test
    void missingRecipeAndLossMakingPriceCannotBeEnabled() {
        Product missingRecipe = product("100000");
        when(recipeRepository.findByProduct(missingRecipe)).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> service.requireSellable(missingRecipe));

        Product lossMaking = product("65000");
        Ingredient ingredient = ingredient("81850");
        when(recipeRepository.findByProduct(lossMaking))
                .thenReturn(List.of(recipe(lossMaking, ingredient, "1")));
        when(menuAvailabilityService.potentialAvailableQuantity(lossMaking)).thenReturn(5);
        assertThrows(ResponseStatusException.class, () -> service.requireSellable(lossMaking));
    }

    private Product product(String price) {
        Product product = new Product();
        product.setId(1);
        product.setPrice(new BigDecimal(price));
        return product;
    }

    private Ingredient ingredient(String unitPrice) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setUnitPrice(new BigDecimal(unitPrice));
        return ingredient;
    }

    private Recipe recipe(Product product, Ingredient ingredient, String amount) {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(new BigDecimal(amount));
        return recipe;
    }
}
