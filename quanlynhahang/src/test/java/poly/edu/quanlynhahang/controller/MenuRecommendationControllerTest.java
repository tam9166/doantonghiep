package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.MenuRecommendationItemResponse;
import poly.edu.quanlynhahang.dto.MenuRecommendationRequest;
import poly.edu.quanlynhahang.entity.CookingMethod;
import poly.edu.quanlynhahang.entity.DietType;
import poly.edu.quanlynhahang.service.GeminiClient;
import poly.edu.quanlynhahang.service.MenuRecommendationService;

class MenuRecommendationControllerTest {
    @Test
    void keepsRuleBasedRecommendationsWhenGeminiIsUnavailable() {
        MenuRecommendationService recommendationService = mock(MenuRecommendationService.class);
        GeminiClient geminiClient = mock(GeminiClient.class);
        MenuRecommendationItemResponse item = new MenuRecommendationItemResponse(
                7, "Canh rau", null, BigDecimal.valueOf(50_000), DietType.CHAY,
                CookingMethod.HAP, 0, "BALANCE_HEAVY_MEAL");
        when(recommendationService.recommend(List.of(1))).thenReturn(List.of(item));
        when(geminiClient.generate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("CUSTOMER_MENU_SUGGESTION")))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        var response = new MenuRecommendationController(recommendationService, geminiClient)
                .recommend(new MenuRecommendationRequest(List.of(1)));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("RULE_BASED", response.getBody().source());
        assertEquals(List.of(item), response.getBody().suggestions());
        assertEquals(null, response.getBody().message());
    }
}
