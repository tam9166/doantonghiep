package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.service.MenuAvailabilityService;

class PreorderMenuControllerTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final MenuAvailabilityService menuAvailabilityService = mock(MenuAvailabilityService.class);
    private final PreorderMenuController controller =
            new PreorderMenuController(productRepository, menuAvailabilityService);

    @Test
    void activeSoldOutDishRemainsVisibleButUnavailableForPreorder() {
        Product product = new Product();
        product.setId(7);
        product.setNameVi("Gỏi sen");
        product.setName("Gỏi sen");
        product.setPrice(new BigDecimal("120000"));
        product.setStatus(true);
        product.setAvailable(true);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(menuAvailabilityService.availableQuantity(product)).thenReturn(0);

        var response = controller.preorderItems();

        assertEquals(1, response.size());
        assertEquals(0, response.getFirst().getAvailableQuantity());
        assertFalse(response.getFirst().getAvailable());
    }
}
