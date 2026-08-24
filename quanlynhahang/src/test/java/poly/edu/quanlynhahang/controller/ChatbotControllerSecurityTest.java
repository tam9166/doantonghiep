package poly.edu.quanlynhahang.controller;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import poly.edu.quanlynhahang.dto.AiRequest;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.service.GeminiClient;
import poly.edu.quanlynhahang.service.StaffOperationsAssistantService;
import poly.edu.quanlynhahang.service.RoleAwareAssistantService;
import poly.edu.quanlynhahang.service.InventoryAlertService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ChatbotControllerSecurityTest {
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final StaffOperationsAssistantService operationsAssistantService = mock(StaffOperationsAssistantService.class);
    private final RoleAwareAssistantService roleAwareAssistantService = mock(RoleAwareAssistantService.class);
    private final ChatbotController controller = new ChatbotController(
            productRepository, geminiClient, operationsAssistantService, roleAwareAssistantService);

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

    @Test
    void inventoryAiAlwaysReceivesTheCanonicalExpiredBatchCount() {
        InventoryAlertService inventoryAlertService = mock(InventoryAlertService.class);
        InventoryAlertService.Analysis analysis = new InventoryAlertService.Analysis(
                List.of(), 1, 1, 1, 24, 0, 0, 1, 0, 0, java.math.BigDecimal.ZERO);
        when(inventoryAlertService.analyze(3)).thenReturn(analysis);
        when(geminiClient.generate(anyString(), org.mockito.ArgumentMatchers.eq("INVENTORY_FORECAST")))
                .thenReturn("[]");
        controller.setInventoryAlertService(inventoryAlertService);

        controller.inventory(new AiRequest("phân tích kho", null, null, null, null));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generate(prompt.capture(), org.mockito.ArgumentMatchers.eq("INVENTORY_FORECAST"));
        assertTrue(prompt.getValue().contains("expiredBatches=24"));
        assertTrue(prompt.getValue().contains("không được kết luận kho an toàn"));
    }
}
