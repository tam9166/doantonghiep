package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Product;

public record OrderProductResponse(Integer id, String name, String image) {
    public static OrderProductResponse from(Product product) {
        return product == null ? null : new OrderProductResponse(product.getId(), product.getName(), product.getImage());
    }
}
