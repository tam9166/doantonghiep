package poly.edu.quanlynhahang.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.dto.MenuPreorderItemResponse;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.service.MenuAvailabilityService;

import java.util.List;
@RestController
public class PreorderMenuController {
    private final ProductRepository productRepository;
    private final MenuAvailabilityService menuAvailabilityService;

    public PreorderMenuController(ProductRepository productRepository, MenuAvailabilityService menuAvailabilityService) {
        this.productRepository = productRepository;
        this.menuAvailabilityService = menuAvailabilityService;
    }

    @GetMapping("/api/menu-items/preorder")
    public List<MenuPreorderItemResponse> preorderItems() {
        return productRepository.findAll().stream()
                .filter(product -> !Boolean.FALSE.equals(product.getStatus()))
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
        int quantity = menuAvailabilityService.availableQuantity(product);
        response.setAvailableQuantity(quantity);
        response.setInventoryManaged(quantity >= 0);
        response.setAvailable(Boolean.TRUE.equals(product.getStatus())
                && Boolean.TRUE.equals(product.getAvailable())
                && (quantity < 0 || quantity > 0));
        return response;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
