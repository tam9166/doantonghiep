package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.Ingredient;

public record IngredientResponse(Long id, String name, BigDecimal quantity, String unit, BigDecimal minStock,
                                 BigDecimal unitPrice, String image, Integer shelfLifeDays) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getQuantity(),
                ingredient.getUnit(), ingredient.getMinStock(), ingredient.getUnitPrice(), ingredient.getImage(),
                ingredient.getShelfLifeDays());
    }
}
