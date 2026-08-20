package poly.edu.quanlynhahang.controller;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.dto.PublicProductResponse;
import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerPrivacyTest {
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final ProductController controller = new ProductController(productRepository, reviewRepository);

    @Test
    void publicCatalogDoesNotExposeCostOrInternalProductFields() throws Exception {
        Category category = new Category();
        category.setId(3);
        category.setName("Mon chinh");

        Product product = new Product();
        product.setId(10);
        product.setName("Com sen");
        product.setPrice(new BigDecimal("125000.00"));
        product.setTaxRate(new BigDecimal("8.00"));
        product.setDescription("Mon an nha hang");
        product.setImage("/uploads/com-sen.jpg");
        product.setStatus(true);
        product.setAvailable(true);
        product.setCostPrice(new BigDecimal("45000.00"));
        product.setCategory(category);

        ReviewRepository.ProductRatingSummary rating = mock(ReviewRepository.ProductRatingSummary.class);
        when(rating.getProductId()).thenReturn(10);
        when(rating.getAverageRating()).thenReturn(4.26);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(reviewRepository.getAverageRatingsByProductIds(List.of(10))).thenReturn(List.of(rating));

        List<PublicProductResponse> response = controller.getAllProducts();
        String json = new ObjectMapper().writeValueAsString(response);

        assertEquals(1, response.size());
        assertEquals(4.3, response.getFirst().averageRating());
        assertEquals("Mon chinh", response.getFirst().category().name());
        assertEquals(new BigDecimal("125000.00"), response.getFirst().price());
        assertTrue(json.contains("\"price\":125000.00"));
        assertFalse(json.contains("costPrice"));
        assertFalse(json.contains("createDate"));
        assertFalse(json.contains("recipe"));
        assertFalse(json.contains("ingredient"));
        verify(reviewRepository).getAverageRatingsByProductIds(List.of(10));
    }

    @Test
    void emptyCatalogDoesNotRunAggregateRatingQuery() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertTrue(controller.getAllProducts().isEmpty());
    }
}
