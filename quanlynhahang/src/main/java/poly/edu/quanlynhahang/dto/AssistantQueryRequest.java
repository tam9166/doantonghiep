package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantQueryRequest(
        @NotBlank(message = "Câu hỏi không được để trống")
        @Size(max = 500, message = "Câu hỏi không được vượt quá 500 ký tự")
        String message,
        @Size(max = 100, message = "Mã hội thoại không hợp lệ")
        String conversationId,
        @Size(max = 8, message = "Ngôn ngữ không hợp lệ")
        String locale) {
}
