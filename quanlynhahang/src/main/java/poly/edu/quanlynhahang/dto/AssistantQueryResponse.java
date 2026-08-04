package poly.edu.quanlynhahang.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AssistantQueryResponse(
        String conversationId,
        String role,
        String intent,
        String reply,
        Map<String, Object> data,
        List<String> suggestions,
        Instant generatedAt,
        String source) {
}
