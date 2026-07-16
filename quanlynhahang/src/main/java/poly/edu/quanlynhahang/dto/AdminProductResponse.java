package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Product;

public record AdminProductResponse(
        Integer id,
        String name,
        Double price,
        Double taxRate,
        String image,
        String description,
        Boolean status,
        Boolean available,
        Double averageRating,
        Double costPrice,
        CategoryResponse category) {

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(), product.getName(), product.getPrice(), product.getTaxRate(),
                product.getImage(), product.getDescription(), product.getStatus(), product.getAvailable(),
                product.getAverageRating(), product.getCostPrice(),
                product.getCategory() == null ? null : CategoryResponse.from(product.getCategory()));
    }
}
