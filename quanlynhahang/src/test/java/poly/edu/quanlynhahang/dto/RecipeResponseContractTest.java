package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;

class RecipeResponseContractTest {

    @Test
    void exposesOnlyTheRecipeFieldsNeededByOperationsScreens() {
        Product product = new Product();
        product.setId(11);
        product.setName("Test product");
        Ingredient ingredient = new Ingredient();
        ingredient.setId(12L);
        ingredient.setName("Test ingredient");
        ingredient.setQuantity(5.0);
        ingredient.setUnit("kg");
        ingredient.setUnitPrice(new BigDecimal("120000"));
        Recipe recipe = new Recipe();
        recipe.setId(13L);
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(0.2);

        RecipeResponse response = RecipeResponse.from(recipe);

        assertEquals(11, response.product().id());
        assertEquals(12L, response.ingredient().id());
        assertEquals(new BigDecimal("120000"), response.ingredient().unitPrice());
        assertEquals(0.2, response.amountRequired());
        assertNull(RecipeResponse.from(new Recipe()).product());
    }
}
