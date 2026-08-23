package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;

class MenuAvailabilityServiceTest {
    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final IngredientBatchRepository ingredientBatchRepository = mock(IngredientBatchRepository.class);
    private final InventoryReservationRepository inventoryReservationRepository = mock(InventoryReservationRepository.class);
    private final MenuAvailabilityService service = new MenuAvailabilityService(
            recipeRepository, productRepository, ingredientBatchRepository, inventoryReservationRepository);

    @Test
    void locksDishWhenAnyRecipeIngredientRunsOut() {
        Product product = product(true);
        Ingredient ingredient = ingredient(0.0);
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 0.2)));
        when(ingredientBatchRepository.sumAvailableByIngredientId(ingredient.getId()))
                .thenReturn(BigDecimal.ZERO);

        service.refreshProduct(product);

        verify(productRepository).save(product);
    }

    @Test
    void reopensDishAfterRestockMakesEveryIngredientSufficient() {
        Product product = product(false);
        Ingredient ingredient = ingredient(5.0);
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 0.2)));
        when(ingredientBatchRepository.sumAvailableByIngredientId(ingredient.getId()))
                .thenReturn(new BigDecimal("5.0"));

        service.refreshProduct(product);

        verify(productRepository).save(product);
    }

    @Test
    void availableQuantityUsesLimitingRecipeBatchTotal() {
        Product product = product(true);
        Ingredient meat = ingredient(2.0);
        Ingredient vegetables = ingredient(0.5);
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(
                recipe(product, meat, 0.2), recipe(product, vegetables, 0.1)));
        when(ingredientBatchRepository.sumAvailableByIngredientId(meat.getId()))
                .thenReturn(new BigDecimal("2.0"));
        when(ingredientBatchRepository.sumAvailableByIngredientId(vegetables.getId()))
                .thenReturn(new BigDecimal("0.5"));

        org.junit.jupiter.api.Assertions.assertEquals(5, service.availableQuantity(product));
    }

    @Test
    void availableQuantitySubtractsActiveOrderHolds() {
        Product product = product(true);
        Ingredient ingredient = ingredient(5.0);
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 0.5)));
        when(ingredientBatchRepository.sumAvailableByIngredientId(ingredient.getId()))
                .thenReturn(new BigDecimal("5.0"));
        when(inventoryReservationRepository.sumActiveReservedByIngredientId(
                org.mockito.ArgumentMatchers.eq(ingredient.getId()),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BigDecimal("2.0"));

        org.junit.jupiter.api.Assertions.assertEquals(6, service.availableQuantity(product));
    }

    private Product product(boolean available) {
        Product product = new Product();
        product.setId(1);
        product.setStatus(true);
        product.setAvailable(available);
        return product;
    }

    private Ingredient ingredient(double quantity) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(Math.round(quantity * 100) + 1);
        ingredient.setQuantity(BigDecimal.valueOf(quantity));
        return ingredient;
    }

    private Recipe recipe(Product product, Ingredient ingredient, double amount) {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(BigDecimal.valueOf(amount));
        return recipe;
    }
}
