package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Size;

public record AiRequest(
        @Size(max = 4_000, message = "Nội dung AI không được vượt quá 4000 ký tự")
        String message,

        @Size(max = 40, message = "Loại yêu cầu AI không hợp lệ")
        String type,

        @Size(max = 12_000, message = "Lịch sử hội thoại quá dài")
        String history,

        @Size(max = 30_000, message = "Dữ liệu menu quá dài")
        String menu,

        @Size(max = 12_000, message = "Danh sách món quá dài")
        String dishes,

        @Size(max = 8, message = "Ngôn ngữ không hợp lệ")
        String locale,
        @Size(max = 80, message = "Mã phiên AI không hợp lệ") String sessionId) {

    public AiRequest(String message, String type, String history, String menu, String dishes) {
        this(message, type, history, menu, dishes, null, null);
    }

    public AiRequest(String message, String type, String history, String menu, String dishes, String locale) {
        this(message, type, history, menu, dishes, locale, null);
    }

    public AiRequest withType(String forcedType) {
        return new AiRequest(message, forcedType, history, menu, dishes, locale, sessionId);
    }
}
