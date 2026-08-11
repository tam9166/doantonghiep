package poly.edu.quanlynhahang.dto;

import java.util.List;

public record MenuRecommendationResponse(
        List<MenuRecommendationItemResponse> suggestions,
        String message,
        String source) {
}
