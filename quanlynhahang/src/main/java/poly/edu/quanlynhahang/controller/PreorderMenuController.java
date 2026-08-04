package poly.edu.quanlynhahang.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.dto.MenuPreorderItemResponse;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;

import java.util.List;
@RestController
public class PreorderMenuController {
    private final ProductRepository productRepository;

    public PreorderMenuController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/api/menu-items/preorder")
    public List<MenuPreorderItemResponse> preorderItems() {
        return productRepository.findAll().stream()
                .filter(product -> !Boolean.FALSE.equals(product.getStatus()))
                .filter(product -> !Boolean.FALSE.equals(product.getAvailable()))
                .map(this::toResponse)
                .toList();
    }

    private MenuPreorderItemResponse toResponse(Product product) {
        MenuPreorderItemResponse response = new MenuPreorderItemResponse();
        response.setId(product.getId());
        response.setNameVi(firstNonBlank(product.getNameVi(), product.getName()));
        response.setNameEn(firstNonBlank(product.getNameEn(), product.getName()));
        response.setCategoryNameVi(product.getCategory() == null ? "" : firstNonBlank(product.getCategory().getNameVi(), product.getCategory().getName()));
        response.setCategoryNameEn(product.getCategory() == null ? "" : firstNonBlank(product.getCategory().getNameEn(), product.getCategory().getName()));
        response.setDescriptionVi(firstNonBlank(product.getDescriptionVi(), product.getDescription()));
        response.setDescriptionEn(firstNonBlank(product.getDescriptionEn(), product.getDescription()));
        response.setPrice(product.getPrice());
        response.setImage(product.getImage());
        response.setAvailable(product.getAvailable());
        return response;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
