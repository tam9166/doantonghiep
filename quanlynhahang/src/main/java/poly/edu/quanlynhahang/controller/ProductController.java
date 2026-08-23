package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.dto.PublicProductResponse;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReviewRepository;
import poly.edu.quanlynhahang.service.MenuAvailabilityService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final MenuAvailabilityService menuAvailabilityService;

    public ProductController(ProductRepository productRepository, ReviewRepository reviewRepository,
                             MenuAvailabilityService menuAvailabilityService) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.menuAvailabilityService = menuAvailabilityService;
    }

    @GetMapping
    public List<PublicProductResponse> getAllProducts() {
        return toPublicResponse(productRepository.findAll());
    }

    @GetMapping("/category/{categoryId}")
    public List<PublicProductResponse> getProductsByCategory(@PathVariable Integer categoryId) {
        return toPublicResponse(productRepository.findByCategoryId(categoryId));
    }

    private List<PublicProductResponse> toPublicResponse(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<Integer> productIds = products.stream().map(Product::getId).toList();
        Map<Integer, ReviewRepository.ProductRatingSummary> ratings = reviewRepository
                .getAverageRatingsByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(
                        ReviewRepository.ProductRatingSummary::getProductId,
                        Function.identity()));

        return products.stream()
                .map(product -> PublicProductResponse.from(product, roundRating(ratings.get(product.getId())),
                        menuAvailabilityService.availableQuantity(product)))
                .toList();
    }

    private double roundRating(ReviewRepository.ProductRatingSummary rating) {
        if (rating == null || rating.getAverageRating() == null) {
            return 0.0;
        }
        return Math.round(rating.getAverageRating() * 10.0) / 10.0;
    }
}
