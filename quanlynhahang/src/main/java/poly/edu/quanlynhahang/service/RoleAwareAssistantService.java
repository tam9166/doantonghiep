package poly.edu.quanlynhahang.service;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Calendar;
import java.util.Date;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.dto.AssistantQueryRequest;
import poly.edu.quanlynhahang.dto.AssistantQueryResponse;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.repository.OrderRepository;

/** Resolves the staff role from Spring Security; the browser never supplies it. */
@Service
public class RoleAwareAssistantService {
    private static final List<String> ROLE_PRECEDENCE = List.of("ADMIN", "MANAGER", "KITCHEN", "CASHIER", "WAITER");

    private final StaffOperationsAssistantService operationsAssistantService;
    private final OrderRepository orderRepository;

    public RoleAwareAssistantService(StaffOperationsAssistantService operationsAssistantService,
                                     OrderRepository orderRepository) {
        this.operationsAssistantService = operationsAssistantService;
        this.orderRepository = orderRepository;
    }

    public AssistantQueryResponse query(Authentication authentication, AssistantQueryRequest request) {
        String role = resolveRole(authentication);
        String intent = resolveIntent(request.message());
        AssistantAnswer answer = answer(role, intent, request.message());
        String conversationId = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString() : request.conversationId();
        return new AssistantQueryResponse(
                conversationId,
                role,
                intent,
                answer.reply(),
                answer.data(),
                suggestionsFor(role),
                Instant.now(),
                "DATABASE");
    }

    private String resolveRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Cần đăng nhập để dùng trợ lý vận hành");
        }
        return ROLE_PRECEDENCE.stream()
                .filter(role -> authentication.getAuthorities().stream()
                        .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority())))
                .findFirst()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Vai trò không được phép dùng trợ lý vận hành"));
    }

    private String resolveIntent(String message) {
        String normalized = java.text.Normalizer.normalize(message, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (normalized.contains("chua thanh toan") || normalized.contains("chua thu")
                || normalized.contains("con no") || normalized.contains("hoa don chua")) {
            return "UNPAID_INVOICE_SUMMARY";
        }
        if (normalized.contains("doanh thu") || normalized.contains("da thu")
                || normalized.contains("thu hom nay")) {
            return "TODAY_COLLECTION_SUMMARY";
        }
        if (normalized.contains("han") || normalized.contains("het han")) return "EXPIRING_INGREDIENT_BATCHES";
        if (normalized.contains("mon") || normalized.contains("thuc don") || normalized.contains("kha dung")) return "AVAILABLE_PRODUCTS";
        return "INVENTORY_SUMMARY";
    }

    private List<String> suggestionsFor(String role) {
        if ("KITCHEN".equals(role)) return List.of("Nguyên liệu nào sắp hết hạn?", "Món nào đang khả dụng?");
        if ("CASHIER".equals(role)) return List.of("Có bao nhiêu hóa đơn chưa thanh toán?", "Món nào đang khả dụng?");
        return List.of("Nguyên liệu nào sắp hết?", "Lô nào sắp hết hạn?", "Món nào đang khả dụng?");
    }

    private AssistantAnswer answer(String role, String intent, String message) {
        if ("UNPAID_INVOICE_SUMMARY".equals(intent)) {
            if (!("CASHIER".equals(role) || "ADMIN".equals(role) || "MANAGER".equals(role))) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Chỉ thu ngân hoặc quản lý được tra cứu hóa đơn chưa thanh toán");
            }
            List<Order> unpaidOrders = orderRepository.findAllWithDetails().stream()
                    .filter(order -> !Integer.valueOf(3).equals(order.getStatus()))
                    .filter(order -> order.getRemainingAmount() != null && order.getRemainingAmount().signum() > 0)
                    .toList();
            BigDecimal remaining = unpaidOrders.stream().map(Order::getRemainingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AssistantAnswer(
                    "Hiện có " + unpaidOrders.size() + " hóa đơn chưa thanh toán, tổng còn phải thu "
                            + remaining.toPlainString() + " đồng.",
                    Map.of("invoiceCount", unpaidOrders.size(), "remainingAmount", remaining));
        }
        if ("TODAY_COLLECTION_SUMMARY".equals(intent)) {
            if (!("CASHIER".equals(role) || "ADMIN".equals(role) || "MANAGER".equals(role))) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Chỉ thu ngân hoặc quản lý được tra cứu tiền đã thu");
            }
            Date startOfDay = startOfToday();
            List<Order> paidOrders = orderRepository.findAllWithDetails().stream()
                    .filter(order -> !Integer.valueOf(3).equals(order.getStatus()))
                    .filter(order -> Boolean.TRUE.equals(order.getIsPaid()))
                    .filter(order -> order.getCreateDate() != null && !order.getCreateDate().before(startOfDay))
                    .toList();
            BigDecimal collected = paidOrders.stream()
                    .map(this::collectedAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AssistantAnswer(
                    "Hôm nay đã thu " + collected.toPlainString() + " đồng từ " + paidOrders.size()
                            + " hóa đơn đã xác nhận thanh toán.",
                    Map.of("paidInvoiceCount", paidOrders.size(), "collectedAmount", collected));
        }
        return new AssistantAnswer(operationsAssistantService.answer(message), Map.of());
    }

    private Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private BigDecimal collectedAmount(Order order) {
        if (order.getPaidAmount() != null && order.getPaidAmount().signum() > 0) {
            return order.getPaidAmount();
        }
        return order.getTotalAmount();
    }

    private record AssistantAnswer(String reply, Map<String, Object> data) {
    }
}
