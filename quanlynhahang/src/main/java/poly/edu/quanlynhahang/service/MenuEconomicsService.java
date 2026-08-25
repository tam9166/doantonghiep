package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.RecipeRepository;

@Service
public class MenuEconomicsService {
    private final RecipeRepository recipeRepository;
    private final MenuAvailabilityService menuAvailabilityService;
    private final RestaurantSettingsService restaurantSettingsService;

    public MenuEconomicsService(RecipeRepository recipeRepository,
                                MenuAvailabilityService menuAvailabilityService,
                                RestaurantSettingsService restaurantSettingsService) {
        this.recipeRepository = recipeRepository;
        this.menuAvailabilityService = menuAvailabilityService;
        this.restaurantSettingsService = restaurantSettingsService;
    }

    @Transactional(readOnly = true)
    public Assessment assess(Product product) {
        List<Recipe> recipes = recipeRepository.findByProduct(product);
        boolean completeRecipe = !recipes.isEmpty() && recipes.stream().allMatch(this::validRecipe);
        BigDecimal cost = recipes.stream().filter(this::validRecipe)
                .map(recipe -> recipe.getIngredient().getUnitPrice().multiply(recipe.getAmountRequired()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        int servings = completeRecipe ? menuAvailabilityService.potentialAvailableQuantity(product) : 0;
        BigDecimal margin = restaurantSettingsService.minimumProfitMarginPercent();
        return new Assessment(cost, servings, completeRecipe, recommendedPrice(cost, margin), margin,
                marginStatus(product.getPrice(), cost));
    }

    @Transactional(readOnly = true)
    public Assessment requireSellable(Product product) {
        Assessment assessment = assess(product);
        if (!assessment.hasRecipe()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Món chưa có công thức định lượng hợp lệ");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(assessment.costPrice()) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Giá bán phải cao hơn giá vốn trước khi mở bán");
        }
        if (assessment.availableServings() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không đủ tồn kho hợp lệ để mở bán món này");
        }
        return assessment;
    }

    private boolean validRecipe(Recipe recipe) {
        return recipe.getIngredient() != null && recipe.getIngredient().getUnitPrice() != null
                && recipe.getAmountRequired() != null && recipe.getAmountRequired().signum() > 0;
    }

    BigDecimal recommendedPrice(BigDecimal costPrice, BigDecimal targetMarginPercent) {
        if (costPrice == null || costPrice.signum() <= 0) return BigDecimal.ZERO.setScale(2);
        BigDecimal divisor = BigDecimal.ONE.subtract(
                targetMarginPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal raw = costPrice.divide(divisor, 0, RoundingMode.CEILING);
        BigDecimal step = new BigDecimal("5000");
        return raw.divide(step, 0, RoundingMode.CEILING).multiply(step).setScale(2);
    }

    private String marginStatus(BigDecimal price, BigDecimal cost) {
        return price == null || price.compareTo(cost) <= 0 ? "NEGATIVE_MARGIN" : "VALID";
    }

    public record Assessment(BigDecimal costPrice, int availableServings, boolean hasRecipe,
                             BigDecimal recommendedPrice, BigDecimal targetMarginPercent,
                             String marginStatus) {}
}
