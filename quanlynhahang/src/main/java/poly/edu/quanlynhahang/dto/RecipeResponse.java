package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.Recipe;

public record RecipeResponse(
        Long id,
        ProductSummary product,
        IngredientSummary ingredient,
        Double amountRequired) {

    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getProduct() == null ? null : new ProductSummary(recipe.getProduct().getId(), recipe.getProduct().getName()),
                recipe.getIngredient() == null ? null : new IngredientSummary(
                        recipe.getIngredient().getId(),
                        recipe.getIngredient().getName(),
                        recipe.getIngredient().getQuantity(),
                        recipe.getIngredient().getUnit(),
                        recipe.getIngredient().getUnitPrice()),
                recipe.getAmountRequired());
    }

    public record ProductSummary(Integer id, String name) {
    }

    public record IngredientSummary(Long id, String name, Double quantity, String unit, BigDecimal unitPrice) {
    }
}
