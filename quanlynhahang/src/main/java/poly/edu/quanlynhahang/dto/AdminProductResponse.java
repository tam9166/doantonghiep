package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.DietType;
import poly.edu.quanlynhahang.entity.CookingMethod;

public record AdminProductResponse(
        Integer id,
        String name,
        BigDecimal price,
        BigDecimal taxRate,
        String image,
        String description,
        Boolean status,
        Boolean available,
        Double averageRating,
        BigDecimal costPrice,
        DietType dietType,
        CookingMethod cookingMethod,
        Integer spicyLevel,
        Boolean isSignatureDish,
        CategoryResponse category) {

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(), product.getName(), money(product.getPrice()), product.getTaxRate(),
                product.getImage(), product.getDescription(), product.getStatus(), product.getAvailable(),
                product.getAverageRating(), money(product.getCostPrice()),
                product.getDietType(), product.getCookingMethod(), product.getSpicyLevel(), product.getIsSignatureDish(),
                product.getCategory() == null ? null : CategoryResponse.from(product.getCategory()));
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
