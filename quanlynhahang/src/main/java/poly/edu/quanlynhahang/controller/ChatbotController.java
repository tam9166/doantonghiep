package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    @PostMapping("/chat")
    public ResponseEntity<?> chatWithAI(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("message");
        String type = payload.get("type"); // "INTERVIEW" hoặc "SUPPORT"
        String history = payload.get("history");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reply", "Bạn cần nhập tin nhắn để chatbot trả lời!"));
        }

        if (geminiApiKey == null || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE") || geminiApiKey.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("reply", "Bot đang ở chế độ bảo trì do chưa có API Key!"));
        }

        String systemPrompt = "";
        String combinedText = "";

        if ("INTERVIEW".equals(type)) {
            systemPrompt = "Bạn là Giám đốc Nhân sự (HR) của nhà hàng FPOLY Restaurant (Đà Nẵng). Nhiệm vụ của bạn là phỏng vấn ứng viên trực tiếp qua đoạn chat này. " +
                           "YÊU CẦU BẮT BUỘC: Hãy đọc Lịch sử chat (nếu có) để biết ứng viên vừa nói gì. Đưa ra 1 nhận xét ngắn gọn và tự nhiên về câu trả lời của ứng viên, sau đó ĐẶT CHỈ 1 CÂU HỎI TIẾP THEO liên quan đến vị trí ứng tuyển, kinh nghiệm hoặc kỹ năng xử lý tình huống nhà hàng. " +
                           "KHÔNG ĐƯỢC dùng ký tự ** (dấu sao). Trả lời thật ngắn gọn, chuyên nghiệp và thân thiện bằng tiếng Việt. Nếu ứng viên nói muốn dừng hoặc không còn gì để hỏi, hãy mời họ để lại thông tin liên hệ.";
            combinedText = systemPrompt + "\n\n";
            if (history != null && !history.trim().isEmpty()) {
                combinedText += "--- LỊCH SỬ CHAT ---\n" + history + "\n--------------------\n\n";
            }
            combinedText += "Ứng viên vừa nói: " + userMessage + "\n\nHãy phản hồi và đặt 1 câu hỏi tiếp theo:";
        } else {
            systemPrompt = "Bạn là trợ lý ảo thân thiện của FPOLY Restaurant (Đà Nẵng). " +
                    "Thông tin nhà hàng: Mở cửa từ 8:00 sáng đến 10:00 tối các ngày trong tuần. " +
                    "Thực đơn chuyên món Á Âu, nổi bật với Bít tết và Hải sản. " +
                    "Địa chỉ: 137 Nguyễn Thị Thập, Liên Chiểu, Đà Nẵng. Hotline: 0347944028. " +
                    "YÊU CẦU BẮT BUỘC: Trả lời tự nhiên như người thật. KHÔNG ĐƯỢC dùng ký tự ** (dấu sao) để in đậm. KHÔNG ĐƯỢC mở đầu bằng câu 'FPOLY Restaurant xin chào bạn' hay tương tự. Hãy vào thẳng vấn đề một cách lịch sự, ngắn gọn và trọng tâm bằng tiếng Việt.";
            combinedText = systemPrompt + "\n\nKhách hàng hỏi: " + userMessage;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\":[{\"text\": \"" + combinedText.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]\n" +
                    "  }]\n" +
                    "}";

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_API_URL + geminiApiKey, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    Map<String, Object> body = response.getBody();
                    java.util.List<?> candidates = (java.util.List<?>) body.get("candidates");
                    Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                    java.util.List<?> parts = (java.util.List<?>) content.get("parts");
                    Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                    String aiReply = (String) firstPart.get("text");
                    aiReply = aiReply.replace("**", "");
                    
                    return ResponseEntity.ok(Map.of("reply", aiReply));
                } catch (Exception e) {
                    return ResponseEntity.ok(Map.of("reply", "Xin lỗi, mình không hiểu ý bạn. Vui lòng gọi Hotline để được hỗ trợ!"));
                }
            }
            return ResponseEntity.ok(Map.of("reply", "Hệ thống AI đang quá tải, vui lòng thử lại sau."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("reply", "Lỗi kết nối tới AI: " + e.getMessage()));
        }
    }
}
