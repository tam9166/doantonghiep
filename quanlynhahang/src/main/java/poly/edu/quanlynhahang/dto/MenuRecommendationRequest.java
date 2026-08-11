package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MenuRecommendationRequest(
        @NotNull @Size(max = 20) List<@NotNull @Positive Integer> productIds) {
}
