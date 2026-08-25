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
        BigDecimal expectedProfit,
        BigDecimal profitMarginPercent,
        BigDecimal recommendedPrice,
        BigDecimal targetMarginPercent,
        String marginStatus,
        Integer availableServings,
        Boolean hasRecipe,
        DietType dietType,
        CookingMethod cookingMethod,
        Integer spicyLevel,
        Boolean isSignatureDish,
        CategoryResponse category) {

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(), product.getName(), money(product.getPrice()), product.getTaxRate(),
                product.getImage(), product.getDescription(), product.getStatus(), product.getAvailable(),
                product.getAverageRating(), money(product.getCostPrice()), null, null,
                null, null, null, 0, false,
                product.getDietType(), product.getCookingMethod(), product.getSpicyLevel(), product.getIsSignatureDish(),
                product.getCategory() == null ? null : CategoryResponse.from(product.getCategory()));
    }

    public static AdminProductResponse from(Product product, BigDecimal costPrice, int availableServings,
                                            boolean hasRecipe, BigDecimal recommendedPrice,
                                            BigDecimal targetMarginPercent, String marginStatus) {
        BigDecimal price = money(product.getPrice());
        BigDecimal cost = money(costPrice);
        BigDecimal profit = price == null || cost == null ? null : price.subtract(cost);
        BigDecimal margin = price == null || price.signum() <= 0 || profit == null ? null
                : profit.multiply(BigDecimal.valueOf(100)).divide(price, 2, RoundingMode.HALF_UP);
        return new AdminProductResponse(
                product.getId(), product.getName(), price, product.getTaxRate(), product.getImage(),
                product.getDescription(), product.getStatus(), product.getAvailable(), product.getAverageRating(),
                cost, money(profit), margin, money(recommendedPrice), targetMarginPercent,
                marginStatus, availableServings, hasRecipe,
                product.getDietType(), product.getCookingMethod(), product.getSpicyLevel(),
                product.getIsSignatureDish(),
                product.getCategory() == null ? null : CategoryResponse.from(product.getCategory()));
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
