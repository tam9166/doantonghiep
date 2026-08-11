package poly.edu.quanlynhahang.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.dto.MenuRecommendationItemResponse;
import poly.edu.quanlynhahang.entity.CookingMethod;
import poly.edu.quanlynhahang.entity.DietType;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.ProductRepository;

/**
 * Chooses real, available menu items deterministically. AI is intentionally
 * kept out of this service so it can never invent a product or affect the
 * recommendation when the AI provider is unavailable.
 */
@Service
public class MenuRecommendationService {
    private static final int MAX_SUGGESTIONS = 5;

    private final ProductRepository productRepository;

    public MenuRecommendationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuRecommendationItemResponse> recommend(List<Integer> requestedProductIds) {
        Set<Integer> selectedIds = requestedProductIds == null ? Set.of() : requestedProductIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<Product> availableProducts = productRepository.findByAvailableTrueAndStatusTrue().stream()
                .filter(product -> product.getId() != null)
                .sorted(Comparator.comparing(Product::getId))
                .toList();

        List<Product> selectedProducts = availableProducts.stream()
                .filter(product -> selectedIds.contains(product.getId()))
                .toList();
        List<Product> candidates = availableProducts.stream()
                .filter(product -> !selectedIds.contains(product.getId()))
                .toList();

        LinkedHashMap<Integer, String> recommended = new LinkedHashMap<>();
        boolean vegetarianCart = !selectedProducts.isEmpty()
                && selectedProducts.stream().allMatch(product -> product.getDietType() == DietType.CHAY);
        boolean hasHeavyDish = selectedProducts.stream().anyMatch(this::isHeavyDish);
        boolean hasSignatureDish = selectedProducts.stream()
                .anyMatch(product -> Boolean.TRUE.equals(product.getIsSignatureDish()));

        // Signature dishes are added first so a broad balance rule cannot crowd
        // them out of the limited response.
        if (!hasSignatureDish) {
            Predicate<Product> compatibleSignature = product -> Boolean.TRUE.equals(product.getIsSignatureDish())
                    && (!vegetarianCart || product.getDietType() == DietType.CHAY)
                    && selectedProducts.stream().noneMatch(selected -> sameMealType(selected, product));
            addMatches(recommended, candidates, compatibleSignature, "SIGNATURE_DISH");
            // A small menu can have only signature dishes of an already selected type.
            // It is still better to recommend a real available signature dish than none.
            if (recommended.size() < MAX_SUGGESTIONS) {
                addMatches(recommended, candidates,
                        product -> Boolean.TRUE.equals(product.getIsSignatureDish())
                                && (!vegetarianCart || product.getDietType() == DietType.CHAY),
                        "SIGNATURE_DISH");
            }
        }

        if (vegetarianCart) {
            addMatches(recommended, candidates,
                    product -> product.getDietType() == DietType.CHAY,
                    "VEGETARIAN_COMPLEMENT");
        }

        if (hasHeavyDish) {
            addMatches(recommended, candidates,
                    product -> product.getDietType() == DietType.CHAY || isVegetableOrSoup(product),
                    "BALANCE_HEAVY_MEAL");
        }

        return recommended.entrySet().stream()
                .map(entry -> findById(availableProducts, entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private void addMatches(LinkedHashMap<Integer, String> recommended, List<Product> candidates,
                            Predicate<Product> condition, String reasonCode) {
        candidates.stream()
                .filter(condition)
                .takeWhile(product -> recommended.size() < MAX_SUGGESTIONS)
                .forEach(product -> recommended.putIfAbsent(product.getId(), reasonCode));
    }

    private MenuRecommendationItemResponse findById(List<Product> products, Integer productId, String reasonCode) {
        return products.stream()
                .filter(product -> productId.equals(product.getId()))
                .findFirst()
                .map(product -> MenuRecommendationItemResponse.from(product, reasonCode))
                .orElse(null);
    }

    private boolean isHeavyDish(Product product) {
        return product.getDietType() == DietType.MAN
                && (product.getCookingMethod() == CookingMethod.NUONG || product.getCookingMethod() == CookingMethod.CHIEN);
    }

    private boolean sameMealType(Product first, Product second) {
        return first.getDietType() == second.getDietType()
                && first.getCookingMethod() == second.getCookingMethod();
    }

    private boolean isVegetableOrSoup(Product product) {
        String categoryName = product.getCategory() == null ? "" : String.join(" ",
                safe(product.getCategory().getName()),
                safe(product.getCategory().getNameVi()),
                safe(product.getCategory().getNameEn())).toLowerCase(java.util.Locale.ROOT);
        return categoryName.contains("rau") || categoryName.contains("canh") || categoryName.contains("salad")
                || categoryName.contains("soup");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
