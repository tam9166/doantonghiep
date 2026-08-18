package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

public record MenuRecommendationRequest(
        @NotNull @Size(max = 20) List<@NotNull @Positive Integer> productIds,
        @Min(1) @Max(100) Integer guestCount,
        @Size(max = 12) List<@Size(max = 40) String> preferences,
        @DecimalMin("0") @DecimalMax("100000000") BigDecimal maxBudget) {
    public MenuRecommendationRequest(List<Integer> productIds) {
        this(productIds, null, List.of(), null);
    }

    public MenuRecommendationRequest(List<Integer> productIds, Integer guestCount, List<String> preferences) {
        this(productIds, guestCount, preferences, null);
    }
}
