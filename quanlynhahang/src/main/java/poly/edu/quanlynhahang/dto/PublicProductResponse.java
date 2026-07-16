package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Category;
import poly.edu.quanlynhahang.entity.Product;

public record PublicProductResponse(
        Integer id,
        String name,
        Double price,
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
                product.getPrice(),
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
}
