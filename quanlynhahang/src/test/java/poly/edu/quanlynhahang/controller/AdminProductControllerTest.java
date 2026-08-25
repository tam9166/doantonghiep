package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.dto.AdminProductResponse;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.CategoryRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.MenuEconomicsService;

class AdminProductControllerTest {
    @Test
    void calculatesRecipeCostWithDecimalPrecision() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ActivityLogService activityLogService = mock(ActivityLogService.class);
        MenuEconomicsService menuEconomicsService = mock(MenuEconomicsService.class);
        AdminProductController controller = new AdminProductController(productRepository, categoryRepository,
                reviewRepository, activityLogService, menuEconomicsService);

        Product product = new Product();
        product.setId(1);
        product.setName("Mon thu nghiem");
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(menuEconomicsService.assess(product)).thenReturn(
                new MenuEconomicsService.Assessment(new BigDecimal("0.3000"), 12, true));
        when(reviewRepository.getAverageRatingByProductId(1)).thenReturn(null);

        List<AdminProductResponse> responses = controller.getProductsForOperations();

        assertEquals(new BigDecimal("0.30"), responses.getFirst().costPrice());
        assertEquals(12, responses.getFirst().availableServings());
        assertEquals(true, responses.getFirst().hasRecipe());
    }
}
