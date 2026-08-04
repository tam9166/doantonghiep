package poly.edu.quanlynhahang.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

/** Keeps customer-facing dish availability aligned with recipes and stock. */
@Service
public class MenuAvailabilityService {
    private static final double EPSILON = 0.000001;

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;

    public MenuAvailabilityService(RecipeRepository recipeRepository,
                                   ProductRepository productRepository) {
        this.recipeRepository = recipeRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void refreshForIngredient(Ingredient ingredient) {
        if (ingredient == null) {
            return;
        }
        recipeRepository.findByIngredient(ingredient).stream()
                .map(Recipe::getProduct)
                .filter(product -> product != null)
                .distinct()
                .forEach(this::refreshProduct);
    }

    @Transactional
    public void refreshProduct(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        List<Recipe> recipes = recipeRepository.findByProduct(product);
        boolean canServe = Boolean.TRUE.equals(product.getStatus())
                && !recipes.isEmpty()
                && recipes.stream().allMatch(this::hasEnoughIngredientForOneDish);
        if (!java.util.Objects.equals(product.getAvailable(), canServe)) {
            product.setAvailable(canServe);
            productRepository.save(product);
        }
    }

    private boolean hasEnoughIngredientForOneDish(Recipe recipe) {
        Ingredient ingredient = recipe.getIngredient();
        return ingredient != null
                && recipe.getAmountRequired() != null
                && recipe.getAmountRequired() > 0
                && ingredient.getQuantity() != null
                && ingredient.getQuantity() + EPSILON >= recipe.getAmountRequired();
    }
}
