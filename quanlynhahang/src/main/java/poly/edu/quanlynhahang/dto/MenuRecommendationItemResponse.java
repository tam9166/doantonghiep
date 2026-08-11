package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.CookingMethod;
import poly.edu.quanlynhahang.entity.DietType;
import poly.edu.quanlynhahang.entity.Product;

public record MenuRecommendationItemResponse(
        Integer productId,
        String name,
        String image,
        BigDecimal price,
        DietType dietType,
        CookingMethod cookingMethod,
        Integer spicyLevel,
        String reasonCode) {

    public static MenuRecommendationItemResponse from(Product product, String reasonCode) {
        return new MenuRecommendationItemResponse(
                product.getId(),
                product.getName(),
                product.getImage(),
                product.getPrice(),
                product.getDietType(),
                product.getCookingMethod(),
                product.getSpicyLevel(),
                reasonCode);
    }
}
