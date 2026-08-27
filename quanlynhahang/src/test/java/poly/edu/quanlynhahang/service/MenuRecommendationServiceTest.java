package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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
import poly.edu.quanlynhahang.dto.MenuRecommendationRequest;

@ExtendWith(MockitoExtension.class)
class MenuRecommendationServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private HotMenuItemService hotMenuItemService;
    @Mock
    private MenuAvailabilityService menuAvailabilityService;

    private MenuRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new MenuRecommendationService(productRepository, hotMenuItemService, menuAvailabilityService);
        when(menuAvailabilityService.availableQuantity(any(Product.class))).thenReturn(10);
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

    @Test
    void pairsGrilledDishWithNonAlcoholicDrinkBeforeBeerWithoutBeerSignal() {
        Product grilledMeat = product(1, DietType.MAN, CookingMethod.NUONG, false, true);
        Product beer = product(2, DietType.MAN, CookingMethod.KHAC, false, true);
        beer.setName("Bia tươi");
        Product cola = product(3, DietType.MAN, CookingMethod.KHAC, false, true);
        cola.setName("Nước ép dưa hấu");
        when(productRepository.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(grilledMeat, beer, cola));

        var result = service.recommend(List.of(1));

        assertTrue(result.stream().anyMatch(item -> item.productId().equals(3)
                && "PAIRING_NON_ALCOHOLIC".equals(item.reasonCode())));
        assertTrue(result.stream().noneMatch(item -> item.productId().equals(2)
                && "PAIRING_GRILLED_OR_FRIED".equals(item.reasonCode())));
    }

    @Test
    void pairsSteamedDishWithAvailableWhiteWine() {
        Product steamedFish = product(1, DietType.MAN, CookingMethod.HAP, false, true);
        Product whiteWine = product(2, DietType.MAN, CookingMethod.KHAC, false, true);
        whiteWine.setName("Rượu vang trắng");
        when(productRepository.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(steamedFish, whiteWine));

        var result = service.recommend(List.of(1));

        assertTrue(result.stream().anyMatch(item -> item.productId().equals(2)
                && "PAIRING_SEAFOOD_OR_STEAMED".equals(item.reasonCode())));
    }

    @Test
    void pairsVegetarianDishWithAvailableTeaOrJuice() {
        Product vegetarianDish = product(1, DietType.CHAY, CookingMethod.XAO, false, true);
        Product juice = product(2, DietType.MAN, CookingMethod.KHAC, false, true);
        juice.setName("Nước ép dưa hấu");
        when(productRepository.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(vegetarianDish, juice));

        var result = service.recommend(List.of(1));

        assertTrue(result.stream().anyMatch(item -> item.productId().equals(2)
                && "PAIRING_VEGETARIAN".equals(item.reasonCode())));
    }

    @Test
    void respectsBudgetPerEstimatedDishAndKeepsOnlyActiveRealProducts() {
        Product affordable = product(2, DietType.MAN, CookingMethod.NUONG, false, true);
        affordable.setPrice(BigDecimal.valueOf(180_000));
        Product expensive = product(3, DietType.MAN, CookingMethod.NUONG, false, true);
        expensive.setPrice(BigDecimal.valueOf(300_000));
        when(productRepository.findByAvailableTrueAndStatusTrue()).thenReturn(List.of(affordable, expensive));

        var result = service.recommend(new MenuRecommendationRequest(
                List.of(), 4, List.of("nướng"), BigDecimal.valueOf(400_000)));

        assertEquals(List.of(2), result.stream().map(item -> item.productId()).toList());
    }

    @Test
    void excludesDishWithNoRemainingServingAndSelectedAllergen() {
        Product soldOutBeef = product(2, DietType.MAN, CookingMethod.NUONG, false, true);
        soldOutBeef.setName("Bò nướng");
        Product peanutChicken = product(3, DietType.MAN, CookingMethod.NUONG, false, true);
        peanutChicken.setName("Gà sốt đậu phộng");
        Product safeChicken = product(4, DietType.MAN, CookingMethod.NUONG, false, true);
        safeChicken.setName("Gà nướng lá chanh");
        when(productRepository.findByAvailableTrueAndStatusTrue())
                .thenReturn(List.of(soldOutBeef, peanutChicken, safeChicken));
        when(menuAvailabilityService.availableQuantity(soldOutBeef)).thenReturn(0);

        var result = service.recommend(new MenuRecommendationRequest(
                List.of(), 2, List.of("nướng"), null, List.of("đậu phộng")));

        assertEquals(List.of(4), result.stream().map(item -> item.productId()).toList());
    }

    @Test
    void appliesAllergyExclusionEvenWithoutTastePreferences() {
        Product peanutChicken = product(3, DietType.MAN, CookingMethod.NUONG, false, true);
        peanutChicken.setName("Gà sốt đậu phộng");
        Product safeChicken = product(4, DietType.MAN, CookingMethod.HAP, false, true);
        safeChicken.setName("Gà hấp lá chanh");
        when(productRepository.findByAvailableTrueAndStatusTrue())
                .thenReturn(List.of(peanutChicken, safeChicken));

        var result = service.recommend(new MenuRecommendationRequest(
                List.of(), 2, List.of(), null, List.of("đậu phộng")));

        assertEquals(List.of(4), result.stream().map(item -> item.productId()).toList());
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
