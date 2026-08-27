package poly.edu.quanlynhahang.service;

import java.util.List;
import java.util.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.InventoryReservationRepository;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;

/** Keeps customer-facing dish availability aligned with recipes and stock. */
@Service
public class MenuAvailabilityService {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public MenuAvailabilityService(RecipeRepository recipeRepository,
                                   ProductRepository productRepository,
                                   IngredientBatchRepository ingredientBatchRepository,
                                   InventoryReservationRepository inventoryReservationRepository) {
        this.recipeRepository = recipeRepository;
        this.productRepository = productRepository;
        this.ingredientBatchRepository = ingredientBatchRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
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
                && (recipes.isEmpty() || availableQuantity(recipes) > 0);
        if (!java.util.Objects.equals(product.getAvailable(), canServe)) {
            product.setAvailable(canServe);
            productRepository.save(product);
        }
    }

    @Transactional(readOnly = true)
    public int availableQuantity(Product product) {
        if (product == null || !Boolean.TRUE.equals(product.getStatus())) return 0;
        return availableQuantity(recipeRepository.findByProduct(product));
    }

    @Transactional(readOnly = true)
    public int potentialAvailableQuantity(Product product) {
        if (product == null || !Boolean.TRUE.equals(product.getStatus())) return 0;
        return availableQuantity(recipeRepository.findByProduct(product));
    }

    private int availableQuantity(List<Recipe> recipes) {
        // A dish without a recipe is intentionally not inventory-managed.
        if (recipes.isEmpty()) return -1;
        long available = Integer.MAX_VALUE;
        for (Recipe recipe : recipes) {
            Ingredient ingredient = recipe.getIngredient();
            BigDecimal required = recipe.getAmountRequired();
            if (ingredient == null || ingredient.getId() == null || required == null || required.signum() <= 0) {
                return 0;
            }
            BigDecimal batchTotal = ingredientBatchRepository.sumAvailableByIngredientId(ingredient.getId());
            if (batchTotal == null || batchTotal.signum() <= 0) return 0;
            BigDecimal reserved = inventoryReservationRepository.sumActiveReservedByIngredientId(
                    ingredient.getId(), InventoryReservationStatus.RESERVED, new Date());
            BigDecimal usable = batchTotal.subtract(reserved == null ? BigDecimal.ZERO : reserved)
                    .max(BigDecimal.ZERO);
            long servings = usable.divide(required, 0, RoundingMode.FLOOR).longValue();
            available = Math.min(available, servings);
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, available));
    }
}
