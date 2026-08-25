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

    public MenuEconomicsService(RecipeRepository recipeRepository,
                                MenuAvailabilityService menuAvailabilityService) {
        this.recipeRepository = recipeRepository;
        this.menuAvailabilityService = menuAvailabilityService;
    }

    @Transactional(readOnly = true)
    public Assessment assess(Product product) {
        List<Recipe> recipes = recipeRepository.findByProduct(product);
        boolean completeRecipe = !recipes.isEmpty() && recipes.stream().allMatch(this::validRecipe);
        BigDecimal cost = recipes.stream().filter(this::validRecipe)
                .map(recipe -> recipe.getIngredient().getUnitPrice().multiply(recipe.getAmountRequired()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        int servings = completeRecipe ? menuAvailabilityService.potentialAvailableQuantity(product) : 0;
        return new Assessment(cost, servings, completeRecipe);
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

    public record Assessment(BigDecimal costPrice, int availableServings, boolean hasRecipe) {}
}
