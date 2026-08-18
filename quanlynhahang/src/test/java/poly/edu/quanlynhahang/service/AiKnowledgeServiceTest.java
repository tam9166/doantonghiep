package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import poly.edu.quanlynhahang.entity.AiKnowledgeSource;
import poly.edu.quanlynhahang.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiKnowledgeServiceTest {
    private final AiKnowledgeSourceRepository sources = mock(AiKnowledgeSourceRepository.class);
    private final AiBrandProfileRepository brands = mock(AiBrandProfileRepository.class);
    private final AiFaqExampleRepository faqs = mock(AiFaqExampleRepository.class);
    private final AiKnowledgeService service = new AiKnowledgeService(sources, brands, faqs);

    @Test void retrievesOnlyMatchingEnabledKnowledge() {
        AiKnowledgeSource source = new AiKnowledgeSource(); source.setTitle("Chính sách thú cưng");
        source.setContent("Khu sân vườn cho phép thú cưng có dây giữ.");
        when(sources.findByEnabledTrueOrderByUpdatedAtDesc()).thenReturn(List.of(source));
        when(faqs.findByEnabledTrue()).thenReturn(List.of());
        assertTrue(service.retrieve("Cho tôi hỏi về thú cưng").contains("dây giữ"));
        assertEquals("", service.retrieve("Có chỗ đậu xe không?"));
    }

    @Test void acceptsTextButRejectsExecutableAndOversizedFiles() throws Exception {
        when(sources.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var text = new MockMultipartFile("file", "policy.md", "text/markdown", "Nội dung chính sách".getBytes(StandardCharsets.UTF_8));
        assertEquals("MD", service.upload(text, "Policy").getType());
        var exe = new MockMultipartFile("file", "payload.exe", "application/octet-stream", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> service.upload(exe, null));
        var huge = new MockMultipartFile("file", "huge.txt", "text/plain", new byte[5 * 1024 * 1024 + 1]);
        assertThrows(IllegalArgumentException.class, () -> service.upload(huge, null));
    }
}
