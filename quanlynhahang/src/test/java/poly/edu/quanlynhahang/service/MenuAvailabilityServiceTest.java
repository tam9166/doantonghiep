package poly.edu.quanlynhahang.service;

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

class MenuAvailabilityServiceTest {
    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final MenuAvailabilityService service = new MenuAvailabilityService(recipeRepository, productRepository);

    @Test
    void locksDishWhenAnyRecipeIngredientRunsOut() {
        Product product = product(true);
        Ingredient ingredient = ingredient(0.0);
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 0.2)));

        service.refreshProduct(product);

        verify(productRepository).save(product);
    }

    @Test
    void reopensDishAfterRestockMakesEveryIngredientSufficient() {
        Product product = product(false);
        Ingredient ingredient = ingredient(5.0);
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 0.2)));

        service.refreshProduct(product);

        verify(productRepository).save(product);
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
        ingredient.setQuantity(quantity);
        return ingredient;
    }

    private Recipe recipe(Product product, Ingredient ingredient, double amount) {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(amount);
        return recipe;
    }
}
