package poly.edu.quanlynhahang.controller;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.quanlynhahang.dto.MenuRecommendationItemResponse;
import poly.edu.quanlynhahang.dto.MenuRecommendationRequest;
import poly.edu.quanlynhahang.dto.MenuRecommendationResponse;
import poly.edu.quanlynhahang.service.GeminiClient;
import poly.edu.quanlynhahang.service.MenuRecommendationService;

@RestController
@RequestMapping("/api/customer/ai")
public class MenuRecommendationController {
    private static final Logger log = LoggerFactory.getLogger(MenuRecommendationController.class);

    private final MenuRecommendationService recommendationService;
    private final GeminiClient geminiClient;

    public MenuRecommendationController(MenuRecommendationService recommendationService, GeminiClient geminiClient) {
        this.recommendationService = recommendationService;
        this.geminiClient = geminiClient;
    }

    @PostMapping("/menu-suggestion")
    public ResponseEntity<MenuRecommendationResponse> recommend(@Valid @RequestBody MenuRecommendationRequest request) {
        List<MenuRecommendationItemResponse> suggestions = recommendationService.recommend(request.productIds());
        if (suggestions.isEmpty()) {
            return ResponseEntity.ok(new MenuRecommendationResponse(List.of(), null, "RULE_BASED"));
        }

        try {
            String message = geminiClient.generate(buildPrompt(suggestions), "CUSTOMER_MENU_SUGGESTION");
            return ResponseEntity.ok(new MenuRecommendationResponse(suggestions, message, "GEMINI"));
        } catch (RuntimeException exception) {
            log.info("Customer menu suggestion is using rule-based fallback: {}", exception.getMessage());
            return ResponseEntity.ok(new MenuRecommendationResponse(suggestions, null, "RULE_BASED"));
        }
    }

    private String buildPrompt(List<MenuRecommendationItemResponse> suggestions) {
        String menuItems = suggestions.stream()
                .map(item -> "- " + item.name() + " (lý do: " + item.reasonCode() + ")")
                .collect(Collectors.joining("\n"));
        return "Bạn là trợ lý thực đơn của nhà hàng Mộc Vị. Chỉ viết một lời gợi ý ngắn, thân thiện bằng tiếng Việt "
                + "(tối đa 45 từ) cho khách. Chỉ được nhắc đúng các món trong danh sách dưới đây, không bịa món mới, "
                + "không dùng markdown hay dấu **.\nDanh sách món đã được hệ thống kiểm tra còn hàng:\n" + menuItems;
    }
}
