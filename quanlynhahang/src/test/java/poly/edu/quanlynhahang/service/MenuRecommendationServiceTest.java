package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import poly.edu.quanlynhahang.entity.CookingMethod;
import poly.edu.quanlynhahang.entity.DietType;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class MenuRecommendationServiceTest {
    @Mock
    private ProductRepository productRepository;

    private MenuRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new MenuRecommendationService(productRepository);
    }

    @Test
    void balancesHeavyMeatDishWithAvailableVegetarianDish() {
        Product grilledMeat = product(1, DietType.MAN, CookingMethod.NUONG, false, true);
        Product vegetarianSalad = product(2, DietType.CHAY, CookingMethod.HAP, false, true);
        Product unavailableVegetarian = product(3, DietType.CHAY, CookingMethod.HAP, false, false);
        when(productRepository.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(
                grilledMeat, vegetarianSalad));

        var result = service.recommend(List.of(1));

        assertEquals(List.of(2), result.stream().map(item -> item.productId()).toList());
        assertEquals("BALANCE_HEAVY_MEAL", result.getFirst().reasonCode());
        assertFalse(result.stream().anyMatch(item -> item.productId().equals(unavailableVegetarian.getId())));
    }

    @Test
    void vegetarianCartNeverReceivesMeatSuggestion() {
        Product selectedVegetarian = product(1, DietType.CHAY, CookingMethod.XAO, false, true);
        Product vegetarianSoup = product(2, DietType.CHAY, CookingMethod.HAP, false, true);
        Product meatDish = product(3, DietType.MAN, CookingMethod.NUONG, false, true);
        when(productRepository.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(
                selectedVegetarian, vegetarianSoup, meatDish));

        var result = service.recommend(List.of(1));

        assertEquals(List.of(2), result.stream().map(item -> item.productId()).toList());
        assertEquals("VEGETARIAN_COMPLEMENT", result.getFirst().reasonCode());
    }

    @Test
    void addsAvailableSignatureDishOfDifferentMealTypeWhenCartHasNoSignature() {
        Product selectedGrilledMeat = product(1, DietType.MAN, CookingMethod.NUONG, false, true);
        Product sameTypeSignature = product(2, DietType.MAN, CookingMethod.NUONG, true, true);
        Product steamedSignature = product(3, DietType.MAN, CookingMethod.HAP, true, true);
        when(productRepository.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(
                selectedGrilledMeat, sameTypeSignature, steamedSignature));

        var result = service.recommend(List.of(1));

        assertTrue(result.stream().anyMatch(item -> item.productId().equals(3)
                && "SIGNATURE_DISH".equals(item.reasonCode())));
    }

    private Product product(int id, DietType dietType, CookingMethod cookingMethod,
                            boolean signature, boolean available) {
        Product product = new Product();
        product.setId(id);
        product.setName("Món " + id);
        product.setPrice(BigDecimal.valueOf(100_000));
        product.setDietType(dietType);
        product.setCookingMethod(cookingMethod);
        product.setIsSignatureDish(signature);
        product.setAvailable(available);
        product.setStatus(true);
        return product;
    }
}
