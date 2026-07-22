package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Product;

public record PublicProductResponse(
        Integer id,
        String name,
        BigDecimal price,
        Double taxRate,
        String image,
        String description,
        Boolean status,
        Boolean available,
        Double averageRating,
        CategorySummary category) {

    public static PublicProductResponse from(Product product, Double averageRating) {
        return new PublicProductResponse(
                product.getId(),
                product.getName(),
                money(product.getPrice()),
                product.getTaxRate(),
                product.getImage(),
                product.getDescription(),
                product.getStatus(),
                product.getAvailable(),
                averageRating,
                CategorySummary.from(product.getCategory()));
    }

    public record CategorySummary(Integer id, String name) {
        private static CategorySummary from(Category category) {
            return category == null ? null : new CategorySummary(category.getId(), category.getName());
        }
    }

    private static BigDecimal money(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
