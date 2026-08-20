package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.dto.AdminProductResponse;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.CategoryRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;

class AdminProductControllerTest {
    @Test
    void calculatesRecipeCostWithDecimalPrecision() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        ActivityLogService activityLogService = mock(ActivityLogService.class);
        AdminProductController controller = new AdminProductController(productRepository, categoryRepository,
                reviewRepository, recipeRepository, activityLogService);

        Product product = new Product();
        product.setId(1);
        product.setName("Mon thu nghiem");
        Ingredient ingredient = new Ingredient();
        ingredient.setUnitPrice(BigDecimal.valueOf(0.1));
        Recipe recipe = new Recipe();
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(new BigDecimal("3.0000"));

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));
        when(reviewRepository.getAverageRatingByProductId(1)).thenReturn(null);

        List<AdminProductResponse> responses = controller.getProductsForOperations();

        assertEquals(new BigDecimal("0.30"), responses.getFirst().costPrice());
    }
}
