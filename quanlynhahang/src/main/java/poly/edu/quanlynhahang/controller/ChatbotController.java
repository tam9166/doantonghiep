package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.dto.AiRequest;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;
import poly.edu.quanlynhahang.service.GeminiClient;

import jakarta.validation.Valid;

import java.util.Map;
import java.util.Locale;
import java.text.Normalizer;
import java.util.Set;
@RestController
public class ChatbotController {
    private static final Set<String> PUBLIC_TYPES = Set.of(
            "SUPPORT", "INTERVIEW", "VOICE_ORDER", "WEATHER_RECOMMEND", "COMBO_RECOMMEND");

    private final ProductRepository productRepository;
    private final GeminiClient geminiClient;
    private TableAreaRepository tableAreaRepository;

    @Value("${restaurant.info.name:Moc Vi Restaurant}")
    private String restaurantName = "Moc Vi Restaurant";

    @Value("${restaurant.info.address:137 Nguyen Thi Thap, Da Nang}")
    private String restaurantAddress = "137 Nguyen Thi Thap, Da Nang";

    @Value("${restaurant.info.hotline:0347944028}")
    private String restaurantHotline = "0347944028";

    @Value("${restaurant.info.email:contact@mocvi.vn}")
    private String restaurantEmail = "contact@mocvi.vn";

    @Value("${restaurant.info.opening-hours:09:00 - 23:00}")
    private String restaurantOpeningHours = "09:00 - 23:00";

    public ChatbotController(ProductRepository productRepository, GeminiClient geminiClient) {
        this.productRepository = productRepository;
        this.geminiClient = geminiClient;
    }

    @Autowired
    void setTableAreaRepository(TableAreaRepository tableAreaRepository) {
        this.tableAreaRepository = tableAreaRepository;
    }

    @PostMapping("/api/chatbot/chat")
    public ResponseEntity<?> chatWithAI(@Valid @RequestBody AiRequest payload) {
        String type = normalizePublicType(payload.type());
        if (!PUBLIC_TYPES.contains(type)) {
            throw new AccessDeniedException("Loại yêu cầu AI này không được phép trên endpoint công khai");
        }
        return generateChat(payload.withType(type));
    }

    @PostMapping("/api/admin/ai/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> analytics(@Valid @RequestBody AiRequest payload) {
        return generateChat(payload.withType("ADMIN_ANALYTICS"));
    }

    @PostMapping("/api/admin/ai/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> inventory(@Valid @RequestBody AiRequest payload) {
        return generateChat(payload.withType("INVENTORY_FORECAST"));
    }

    @PostMapping("/api/admin/ai/customer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> customer(@Valid @RequestBody AiRequest payload) {
        return generateChat(payload.withType("CUSTOMER_ANALYTICS"));
    }

    @PostMapping("/api/staff/ai/waiter")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<?> waiter(@Valid @RequestBody AiRequest payload) {
        return generateChat(payload.withType("WAITER_UPSELL"));
    }

    private ResponseEntity<?> generateChat(AiRequest payload) {
        String userMessage = payload.message();
        String type = payload.type();
        String history = payload.history();
        boolean english = "en".equalsIgnoreCase(payload.locale());

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("reply", english
                    ? "Please enter a message for the chatbot."
                    : "Bạn cần nhập tin nhắn để chatbot trả lời!"));
        }

        if ("SUPPORT".equals(type)) {
            String directReply = directSupportReply(userMessage, english);
            if (directReply != null) {
                return ResponseEntity.ok(Map.of("reply", directReply));
            }
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
            String menu = payload.menu();
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
            String menu = payload.menu();
            systemPrompt = "Bạn là Trợ lý AI gợi ý món ăn theo thời tiết của Mộc Vị Restaurant. Bạn sẽ nhận được tình trạng Thời tiết hiện tại và Menu nhà hàng. "
                    +
                    "Nhiệm vụ: Phân tích thời tiết và CHỌN RA ĐÚNG 2 MÓN ĂN phù hợp nhất. VD: Trời lạnh/mưa thì chọn Lẩu, Nướng, Cay nóng. Trời nóng thì chọn Nước ép, Đồ mát. "
                    +
                    "YÊU CẦU BẮT BUỘC: TRẢ VỀ DUY NHẤT 1 MẢNG JSON hợp lệ. Định dạng JSON: [{\"id\": 1, \"reason\": \"Trời mưa lạnh rất hợp ăn Lẩu Thái chua cay\"}]. KHÔNG trả lời thêm bất kỳ từ nào. KHÔNG dùng ```json.";
            combinedText = systemPrompt + "\n\nDanh sách Menu:\n" + menu + "\n\nThời tiết hiện tại ở nhà hàng: "
                    + userMessage + "\n\nHãy trả về JSON:";
        } else if ("COMBO_RECOMMEND".equals(type)) {
            String menu = payload.menu();
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
                    .filter(p -> Boolean.TRUE.equals(p.getStatus()) && Boolean.TRUE.equals(p.getAvailable()))
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

        return ResponseEntity.ok(Map.of("reply", geminiClient.generate(combinedText, type)));
    }

    @PostMapping("/api/staff/ai/kitchen")
    @PreAuthorize("hasAnyRole('KITCHEN', 'ADMIN', 'MANAGER')")
    public ResponseEntity<?> suggestKitchenOrder(@Valid @RequestBody AiRequest payload) {
        String dishesList = payload.dishes();
        if (dishesList == null || dishesList.isBlank()) {
            return ResponseEntity.ok(Map.of("reply", "Chưa có món nào để gợi ý."));
        }

        String prompt = "Bạn là Bếp Trưởng AI của Mộc Vị Restaurant. Hãy phân tích danh sách các món cần nấu sau đây và đề xuất thứ tự ưu tiên làm món nào trước, món nào sau để tối ưu hóa thời gian (Ví dụ: các món cùng loại có thể nấu chung 1 chảo, món lâu nấu trước...). Hãy trả lời RẤT NGẮN GỌN (3-4 dòng) với gạch đầu dòng.\nDanh sách: "
                + dishesList;
        return ResponseEntity.ok(Map.of("reply", geminiClient.generate(prompt, "KITCHEN")));
    }

    private String normalizePublicType(String type) {
        if (type == null || type.isBlank()) return "SUPPORT";
        return type.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String directSupportReply(String message, boolean english) {
        String normalized = normalizeText(message);
        if (containsAny(normalized, "dia chi", "o dau", "address", "located", "location")) {
            return english
                    ? restaurantName + " is located at " + restaurantAddress + "."
                    : restaurantName + " nằm tại " + restaurantAddress + ".";
        }
        if (containsAny(normalized, "hotline", "so dien thoai", "so lien he", "phone number", "contact number")) {
            return english
                    ? "Our hotline is " + restaurantHotline + "."
                    : "Hotline của nhà hàng là " + restaurantHotline + ".";
        }
        if (containsAny(normalized, "mo cua", "dong cua", "gio hoat dong", "opening hours", "open hours", "what time")) {
            return english
                    ? "Our opening hours are " + restaurantOpeningHours + " daily."
                    : "Nhà hàng mở cửa " + restaurantOpeningHours + " hằng ngày.";
        }
        if (containsAny(normalized, "email")) {
            return english
                    ? "You can contact us at " + restaurantEmail + "."
                    : "Email liên hệ của nhà hàng là " + restaurantEmail + ".";
        }
        if (containsAny(normalized, "thuc don", "menu")) {
            return english
                    ? "You can view the current menu on the Menu page. I only recommend dishes that are currently available."
                    : "Bạn có thể xem thực đơn hiện tại tại trang Thực đơn. Tôi chỉ tư vấn các món đang phục vụ.";
        }
        if (containsAny(normalized, "dat ban", "book a table", "reservation")) {
            return english
                    ? "Please open the Reservation page to choose your date, time, area, and available table."
                    : "Bạn hãy mở trang Đặt bàn để chọn ngày, giờ, khu vực và bàn còn trống.";
        }
        if (containsAny(normalized, "du lieu chatbot", "chatbot data", "menu o dau", "menu from")) {
            return english
                    ? "Yes. The chatbot uses the restaurant's configured information and current menu data from this system."
                    : "Có. Chatbot sử dụng thông tin đã cấu hình và dữ liệu thực đơn hiện có trong hệ thống nhà hàng.";
        }
        if (containsAny(normalized, "khu vuc", "areas", "area")) {
            return areaReply(english);
        }
        return null;
    }

    private String areaReply(boolean english) {
        if (tableAreaRepository == null) {
            return english
                    ? "Area information is not currently configured. Please contact our hotline at " + restaurantHotline + "."
                    : "Thông tin khu vực hiện chưa được cấu hình. Vui lòng liên hệ hotline " + restaurantHotline + " để xác nhận.";
        }
        var areas = tableAreaRepository.findByStatusOrderByNameViAsc("ACTIVE");
        if (areas.isEmpty()) {
            return english ? "There are no active dining areas at the moment." : "Hiện chưa có khu vực phục vụ nào đang hoạt động.";
        }
        String names = areas.stream()
                .map(area -> english && area.getNameEn() != null && !area.getNameEn().isBlank()
                        ? area.getNameEn() : area.getNameVi())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return english ? "Our active dining areas are: " + names + "." : "Các khu vực đang phục vụ: " + names + ".";
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) return true;
        }
        return false;
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
