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

    @org.springframework.beans.factory.annotation.Autowired
    private poly.edu.quanlynhahang.repository.ProductRepository productRepository;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

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
            systemPrompt = "Bạn là Giám đốc Nhân sự (HR) của nhà hàng Mộc Vị Restaurant (Đà Nẵng). Nhiệm vụ của bạn là phỏng vấn ứng viên trực tiếp qua đoạn chat này. "
                    +
                    "YÊU CẦU BẮT BUỘC: Hãy đọc Lịch sử chat (nếu có) để biết ứng viên vừa nói gì. Đưa ra 1 nhận xét ngắn gọn và tự nhiên về câu trả lời của ứng viên, sau đó ĐẶT CHỈ 1 CÂU HỎI TIẾP THEO liên quan đến vị trí ứng tuyển, kinh nghiệm hoặc kỹ năng xử lý tình huống nhà hàng. "
                    +
                    "KHÔNG ĐƯỢC dùng ký tự ** (dấu sao). Trả lời thật ngắn gọn, chuyên nghiệp và thân thiện bằng tiếng Việt. Nếu ứng viên nói muốn dừng hoặc không còn gì để hỏi, hãy mời họ để lại thông tin liên hệ.";
            combinedText = systemPrompt + "\n\n";
            if (history != null && !history.trim().isEmpty()) {
                combinedText += "--- LỊCH SỬ CHAT ---\n" + history + "\n--------------------\n\n";
            }
            combinedText += "Ứng viên vừa nói: " + userMessage + "\n\nHãy phản hồi và đặt 1 câu hỏi tiếp theo:";
        } else if ("ADMIN_ANALYTICS".equals(type)) {
            systemPrompt = "Bạn là Giám đốc Tài chính AI của Mộc Vị Restaurant. Nhiệm vụ của bạn là nhận dữ liệu thống kê tài chính (JSON) và viết 1 đoạn đánh giá ngắn gọn (dưới 150 chữ) bằng tiếng Việt. "
                    +
                    "Dữ liệu sẽ bao gồm: doanh_thu_tong (Đầu ra), gia_von_tong (Đầu vào), loi_nhuan_tong (= doanh thu - giá vốn), bien_loi_nhuan (tỷ lệ %), và top_5_mon (kèm doanh thu + giá vốn + lợi nhuận từng món). "
                    +
                    "Hãy phân tích: 1) Biên lợi nhuận tốt hay xấu (tiêu chuẩn ngành F&B là 60-70%). 2) Món nào có lợi nhuận cao nhất, thấp nhất. 3) Đề xuất giải pháp tối ưu. "
                    +
                    "Không dùng ký tự **. Viết như một CFO thực thụ.";
            combinedText = systemPrompt + "\n\nDữ liệu tài chính: " + userMessage
                    + "\n\nHãy phân tích và cho lời khuyên chiến lược:";
        } else if ("KITCHEN_SORT".equals(type)) {
            systemPrompt = "Bạn là Bếp Trưởng AI của Mộc Vị Restaurant. Bạn sẽ nhận danh sách các đơn hàng (món ăn và số lượng) đang chờ nấu. "
                    +
                    "Nhiệm vụ của bạn là hướng dẫn nhân viên bếp gom các món giống nhau lại để nấu chung 1 lượt nhằm tiết kiệm thời gian. Trả lời cực kỳ ngắn gọn (dưới 50 chữ), gạch đầu dòng rõ ràng, phong cách đốc thúc, khẩn trương. Không dùng ký tự **.";
            combinedText = systemPrompt + "\n\nDanh sách đơn chờ: " + userMessage + "\n\nHãy gợi ý gom món nấu chung:";
        } else if ("WAITER_UPSELL".equals(type)) {
            systemPrompt = "Bạn là Chuyên gia Bán chéo (Upsell) AI của Mộc Vị Restaurant. Bạn sẽ nhận danh sách món khách đang ăn tại 1 bàn. "
                    +
                    "Nhiệm vụ của bạn là gợi ý cho Phục vụ 1-2 món đồ uống hoặc tráng miệng (như Bia, Rượu vang, Nước ép, Trái cây, Bánh ngọt) phù hợp nhất với các món đó để ra mời khách gọi thêm. Trả lời như đang nói chuyện với Phục vụ, ngắn gọn (dưới 50 chữ), ví dụ: 'Khách đang ăn Lẩu, bạn ra mời thêm Nước ép dưa hấu hoặc Bia tươi đi!'. Không dùng ký tự **.";
            combinedText = systemPrompt + "\n\nBàn này đang ăn: " + userMessage + "\n\nHãy gợi ý 1 món mời thêm:";
        } else if ("VOICE_ORDER".equals(type)) {
            String menu = payload.get("menu");
            systemPrompt = "Bạn là Trợ lý Gọi món AI của Mộc Vị Restaurant. Bạn sẽ nhận được giọng nói (văn bản) của khách hàng yêu cầu gọi món và danh sách Menu của nhà hàng. "
                    +
                    "Nhiệm vụ của bạn là trích xuất các món khách muốn gọi, đối chiếu với danh sách Menu để lấy ID món ăn. "
                    +
                    "YÊU CẦU BẮT BUỘC: CHỈ ĐƯỢC PHÉP TRẢ VỀ DUY NHẤT 1 MẢNG JSON hợp lệ. Không trả lời bất kỳ câu nào khác. "
                    +
                    "Định dạng JSON phải là: [{\"productId\": 1, \"quantity\": 2, \"note\": \"ít đá\"}]. " +
                    "Nếu khách gọi món không có trong Menu, hãy bỏ qua món đó. Nếu không nhận dạng được món nào, trả về mảng rỗng [].";
            combinedText = systemPrompt + "\n\nDanh sách Menu hiện có:\n" + menu + "\n\nKhách hàng nói:\n" + userMessage
                    + "\n\nHãy trả về JSON:";
        } else if ("INVENTORY_FORECAST".equals(type)) {
            systemPrompt = "Bạn là Chuyên gia Quản lý Chuỗi cung ứng AI của Mộc Vị Restaurant. " +
                    "Bạn sẽ nhận được danh sách các nguyên liệu hiện đang sắp hết (Dưới mức Min Stock) cùng với tồn kho hiện tại. "
                    +
                    "Nhiệm vụ của bạn là phân tích và đưa ra Dự báo số lượng CẦN NHẬP KHO cho từng nguyên liệu đó, kèm theo lý do ngắn gọn. "
                    +
                    "YÊU CẦU BẮT BUỘC: TRẢ VỀ DUY NHẤT 1 MẢNG JSON. Định dạng JSON: [{\"name\": \"Thịt bò thăn\", \"suggestedAmount\": 20, \"unit\": \"Kg\", \"reason\": \"Tồn kho sắp hết, cần nhập thêm\"}]. Không dùng ```json.";
            combinedText = systemPrompt + "\n\nDữ liệu tồn kho sắp hết:\n" + userMessage
                    + "\n\nHãy trả về JSON đề xuất nhập kho:";
        } else if ("WEATHER_RECOMMEND".equals(type)) {
            // Nhận thời tiết (vd: Trời mưa lạnh, 23 độ) -> Trả về ID 2 món phù hợp nhất
            String menu = payload.get("menu");
            systemPrompt = "Bạn là Trợ lý AI gợi ý món ăn theo thời tiết của Mộc Vị Restaurant. Bạn sẽ nhận được tình trạng Thời tiết hiện tại và Menu nhà hàng. "
                    +
                    "Nhiệm vụ: Phân tích thời tiết và CHỌN RA ĐÚNG 2 MÓN ĂN phù hợp nhất. VD: Trời lạnh/mưa thì chọn Lẩu, Nướng, Cay nóng. Trời nóng thì chọn Nước ép, Đồ mát. "
                    +
                    "YÊU CẦU BẮT BUỘC: TRẢ VỀ DUY NHẤT 1 MẢNG JSON hợp lệ. Định dạng JSON: [{\"id\": 1, \"reason\": \"Trời mưa lạnh rất hợp ăn Lẩu Thái chua cay\"}]. KHÔNG trả lời thêm bất kỳ từ nào. KHÔNG dùng ```json.";
            combinedText = systemPrompt + "\n\nDanh sách Menu:\n" + menu + "\n\nThời tiết hiện tại ở nhà hàng: "
                    + userMessage + "\n\nHãy trả về JSON:";
        } else if ("COMBO_RECOMMEND".equals(type)) {
            String menu = payload.get("menu");
            systemPrompt = "Bạn là Trợ lý AI tư vấn Combo món ăn của Mộc Vị Restaurant. Bạn sẽ nhận được thông tin số lượng khách, Thời tiết hiện tại và Menu nhà hàng. "
                    +
                    "Nhiệm vụ: Dựa vào số người và thời tiết, hãy thiết kế 1 COMBO PHONG PHÚ, ĐA DẠNG và hợp lý nhất bao gồm nhiều loại món ăn khác nhau (khai vị, món chính, tráng miệng) và nước uống. "
                    +
                    "QUY TẮC CHỌN MÓN: Số lượng MÓN ĂN KHÁC NHAU tối thiểu = ceil(số người * 0.7), tối thiểu 3 món ăn khác nhau. Mỗi món có thể gọi quantity > 1 nếu cần. "
                    +
                    "Luôn kèm nước uống (1 nước/người). VD: 2 người = 2-3 món ăn + 2 nước. 4 người = 3-4 món ăn + 4 nước. 6 người = 4-5 món ăn + 6 nước. 10 người = 7-8 món ăn + 10 nước. "
                    +
                    "Chọn NHIỀU LOẠI khác nhau (Khai vị, Nướng, Lẩu, Hải sản, Cơm, Salad...) để bữa tiệc thú vị. "
                    +
                    "YÊU CẦU BẮT BUỘC: TRẢ VỀ DUY NHẤT 1 MẢNG JSON. Định dạng JSON: [{\"id\": 1, \"quantity\": 2, \"reason\": \"Trời mưa nên ăn 1 lẩu và 2 trà ấm cho 2 người...\"}]. Chỉ cần ghi 'reason' ở item đầu tiên. KHÔNG dùng ```json hay markdown.";
            combinedText = systemPrompt + "\n\nDanh sách Menu:\n" + menu + "\n\nYêu cầu của khách: " + userMessage
                    + "\n\nHãy trả về JSON Combo:";
        } else if ("CUSTOMER_ANALYTICS".equals(type)) {
            systemPrompt = "Bạn là Chuyên gia Chăm sóc Khách hàng AI của Mộc Vị Restaurant. Bạn sẽ nhận được dữ liệu (JSON) gồm thông tin hạng thẻ hiện tại của khách và danh sách các hóa đơn họ đã tiêu dùng. "
                    +
                    "Nhiệm vụ của bạn là: 1) Tính tổng số tiền khách đã chi tiêu (tất cả các hóa đơn hoàn thành). 2) Đánh giá thói quen tiêu dùng (khách hay ăn gì, khung giờ nào). 3) Đưa ra lời khuyên cho Quản lý nhà hàng về cách tri ân hoặc chăm sóc khách này dựa trên hạng thẻ của họ. "
                    +
                    "Viết bằng tiếng Việt, chia làm 3 ý rõ ràng (Tổng chi tiêu, Phân tích thói quen, Đề xuất chăm sóc), cực kỳ ngắn gọn súc tích (khoảng 100 chữ). Không dùng ký tự **.";
            combinedText = systemPrompt + "\n\nDữ liệu khách hàng và lịch sử hóa đơn: " + userMessage
                    + "\n\nHãy phân tích khách hàng này:";
        } else {
            // Lấy danh sách menu (tối đa 20 món để không quá dài)
            String menuStr = productRepository.findAll().stream()
                    .filter(p -> p.getStatus() != null && p.getStatus())
                    .limit(20)
                    .map(p -> p.getName() + " (" + p.getPrice() + "đ)")
                    .reduce((a, b) -> a + ", " + b).orElse("Đang cập nhật");

            systemPrompt = "Bạn là trợ lý ảo thân thiện của Mộc Vị Restaurant. Bạn không chỉ giúp đặt bàn mà còn là người tư vấn nhiệt tình.\n"
                    +
                    "THÔNG TIN NHÀ HÀNG: Mở cửa 9:00-23:00. Địa chỉ: 137 Nguyễn Thị Thập. Hotline: 0347944028. Các khu vực: Tầng 2 (Sảnh tiệc 15-30 bàn), Tầng 3-4-5 (Phòng VIP 4-10 người), Tầng 6 (Sân thượng lãng mạn).\n"
                    +
                    "THỰC ĐƠN NỔI BẬT: " + menuStr + "\n" +
                    "NHIỆM VỤ CỦA BẠN:\n" +
                    "1. Giao tiếp tự nhiên, trả lời các câu hỏi về thực đơn, giờ giấc, địa chỉ, không gò ép khách phải đặt bàn nếu họ chưa muốn.\n"
                    +
                    "2. Nếu khách chủ động ngỏ ý muốn đặt bàn, hãy hướng dẫn họ cung cấp 3 thông tin (Thời gian, Số lượng người, Vị trí/View mong muốn).\n"
                    +
                    "3. ĐẶC BIỆT: NẾU và CHỈ NẾU khách ĐÃ ĐƯA ĐỦ 3 THÔNG TIN đặt bàn, BẠN KHÔNG TRẢ LỜI BẰNG LỜI VĂN BÌNH THƯỜNG, mà hãy xuất CHÍNH XÁC dòng lệnh này: [ACTION:BOOK_TABLE|time=THỜI GIAN|pax=SỐ NGƯỜI|view=VỊ TRÍ]. Ví dụ: [ACTION:BOOK_TABLE|time=19:00|pax=2|view=View sông]\n"
                    +
                    "4. ĐẶC BIỆT: NẾU khách yêu cầu muốn xem chi tiết Toàn bộ Menu, hãy xuất câu này VÀO CUỐI CÂU TRẢ LỜI: [ACTION:SHOW_MENU]\n"
                    +
                    "KHÔNG dùng ký tự ** (dấu sao). Trả lời ngắn gọn, thân thiện.";

            combinedText = systemPrompt + "\n\n";
            if (history != null && !history.trim().isEmpty()) {
                combinedText += "--- LỊCH SỬ CHAT ---\n" + history + "\n--------------------\n\n";
            }
            combinedText += "Khách hàng vừa nói: " + userMessage + "\n\nHãy phản hồi tự nhiên:";
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\":[{\"text\": \"" + combinedText.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]\n"
                    +
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
                    return ResponseEntity.ok(
                            Map.of("reply", "Xin lỗi, mình không hiểu ý bạn. Vui lòng gọi Hotline để được hỗ trợ!"));
                }
            }
            return ResponseEntity.ok(Map.of("reply", "Hệ thống AI đang quá tải, vui lòng thử lại sau."));
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                return ResponseEntity.ok(
                        Map.of("reply", "AI đang quá tải do vượt giới hạn truy cập. Vui lòng thử lại sau 30 giây!"));
            }
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("reply",
                    "AI tạm thời không khả dụng (Lỗi " + e.getStatusCode().value() + "). Vui lòng thử lại sau!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .ok(Map.of("reply", "Không thể kết nối tới AI. Vui lòng kiểm tra kết nối mạng và thử lại!"));
        }
    }

    @PostMapping("/kitchen/suggest")
    public ResponseEntity<?> suggestKitchenOrder(@RequestBody Map<String, String> payload) {
        String dishesList = payload.get("dishes");
        if (dishesList == null || dishesList.isBlank()) {
            return ResponseEntity.ok(Map.of("reply", "Chưa có món nào để gợi ý."));
        }

        try {
            String prompt = "Bạn là Bếp Trưởng AI của Mộc Vị Restaurant. Hãy phân tích danh sách các món cần nấu sau đây và đề xuất thứ tự ưu tiên làm món nào trước, món nào sau để tối ưu hóa thời gian (Ví dụ: các món cùng loại có thể nấu chung 1 chảo, món lâu nấu trước...). Hãy trả lời RẤT NGẮN GỌN (3-4 dòng) với gạch đầu dòng.\nDanh sách: "
                    + dishesList;

            String requestBody = "{"
                    + "\"contents\": [{"
                    + "\"parts\": [{\"text\": \"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]"
                    + "}]"
                    + "}";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody,
                    headers);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(GEMINI_API_URL + geminiApiKey, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                java.util.List<?> candidates = (java.util.List<?>) body.get("candidates");
                Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                java.util.List<?> parts = (java.util.List<?>) content.get("parts");
                Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                String aiReply = (String) firstPart.get("text");
                return ResponseEntity.ok(Map.of("reply", aiReply.replace("**", "")));
            }
            return ResponseEntity.ok(Map.of("reply", "AI đang bận, vui lòng tự điều phối thủ công."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("reply", "Không thể kết nối AI gợi ý bếp."));
        }
    }
}
