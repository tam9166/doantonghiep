package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Category;

public record CategoryResponse(Integer id, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
