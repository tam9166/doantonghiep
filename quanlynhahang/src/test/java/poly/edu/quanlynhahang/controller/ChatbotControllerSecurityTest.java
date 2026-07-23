package poly.edu.quanlynhahang.controller;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import poly.edu.quanlynhahang.dto.AiRequest;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.service.GeminiClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatbotControllerSecurityTest {
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final ChatbotController controller = new ChatbotController(productRepository, geminiClient);

    @Test
    void publicEndpointRejectsInternalUseCaseBeforeCallingGemini() {
        AiRequest request = new AiRequest("financial data", "ADMIN_ANALYTICS", null, null, null);

        assertThrows(AccessDeniedException.class, () -> controller.chatWithAI(request));

        verifyNoInteractions(geminiClient);
        verifyNoInteractions(productRepository);
    }

    @Test
    void publicSupportOpeningHoursUsesConfiguredResponseWithoutGemini() {
        var response = controller.chatWithAI(new AiRequest("Mấy giờ mở cửa?", "SUPPORT", null, null, null));

        assertEquals("Nhà hàng mở cửa 09:00 - 23:00 hằng ngày.", ((java.util.Map<?, ?>) response.getBody()).get("reply"));
        verifyNoInteractions(geminiClient);
    }

    @Test
    void publicSupportHotlineUsesEnglishWhenRequested() {
        var response = controller.chatWithAI(new AiRequest(
                "What is the hotline number?", "SUPPORT", null, null, null, "en"));

        assertEquals("Our hotline is 0347944028.", ((java.util.Map<?, ?>) response.getBody()).get("reply"));
        verifyNoInteractions(geminiClient);
    }

    @Test
    void oversizedInputFailsBeanValidation() {
        String oversized = "x".repeat(4_001);
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(
                    new AiRequest(oversized, "SUPPORT", null, null, null));
            assertFalse(violations.isEmpty());
        }
    }
}
