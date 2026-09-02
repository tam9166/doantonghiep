package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.AdminProductResponse;
import poly.edu.quanlynhahang.dto.ProductUpsertRequest;
import poly.edu.quanlynhahang.entity.Category;
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
                new MenuEconomicsService.Assessment(new BigDecimal("0.3000"), 12, true,
                        new BigDecimal("5000.00"), new BigDecimal("30.00"), "VALID"));
        when(reviewRepository.getAverageRatingByProductId(1)).thenReturn(null);

        List<AdminProductResponse> responses = controller.getProductsForOperations();

        assertEquals(new BigDecimal("0.30"), responses.getFirst().costPrice());
        assertEquals(12, responses.getFirst().availableServings());
        assertEquals(true, responses.getFirst().hasRecipe());
    }

    @ParameterizedTest
    @CsvSource({
            "90000,103499,false",
            "90000,103500,true",
            "100000,109999,false",
            "100000,110000,true",
            "999999,1099998,false",
            "999999,1099999,true",
            "1000000,1049999,false",
            "1000000,1050000,true"
    })
    void enforcesTieredMinimumSalePriceFromCostPrice(String cost, String price, boolean valid) {
        ProductRepository productRepository = mock(ProductRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ReviewRepository reviewRepository = mock(ReviewRepository.class);
        ActivityLogService activityLogService = mock(ActivityLogService.class);
        MenuEconomicsService menuEconomicsService = mock(MenuEconomicsService.class);
        AdminProductController controller = new AdminProductController(productRepository, categoryRepository,
                reviewRepository, activityLogService, menuEconomicsService);
        Category category = new Category();
        category.setId(3);
        when(categoryRepository.findById(3)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(99);
            return product;
        });

        ProductUpsertRequest request = productRequest(new BigDecimal(price), new BigDecimal(cost));

        if (valid) {
            AdminProductResponse response = controller.addProduct(request).getBody();
            assertEquals(new BigDecimal(cost + ".00"), response.costPrice());
        } else {
            assertThrows(ResponseStatusException.class, () -> controller.addProduct(request));
            verify(productRepository, never()).save(any());
        }
    }

    private ProductUpsertRequest productRequest(BigDecimal price, BigDecimal costPrice) {
        return new ProductUpsertRequest("Mon thu nghiem", price, costPrice, new BigDecimal("8.00"),
                "/images/products/test.jpg", "Mo ta", false, false,
                null, null, 0, false, new ProductUpsertRequest.CategoryReference(3));
    }
}
