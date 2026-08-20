package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Notification;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.NotificationRepository;

import java.util.*;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientBatchRepository ingredientBatchRepository;

    /**
     * Tạo thông báo mới
     */
    public Notification createNotification(String type, String title, String message,
                                           String targetRole, String severity,
                                           String relatedEntity, String relatedId) {
        // Kiểm tra trùng lặp: không tạo thông báo giống hệt nếu chưa đọc
        List<Notification> existing = notificationRepository
                .findByTypeAndRelatedEntityAndRelatedIdAndIsReadFalse(type, relatedEntity, relatedId);
        if (!existing.isEmpty()) {
            return existing.get(0); // Đã có thông báo tương tự chưa đọc
        }

        Notification n = new Notification();
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setTargetRole(targetRole);
        n.setSeverity(severity);
        n.setRelatedEntity(relatedEntity);
        n.setRelatedId(relatedId);
        n.setCreatedAt(new Date());
        return notificationRepository.save(n);
    }

    /**
     * Kiểm tra toàn bộ hệ thống và tạo thông báo cảnh báo
     * Gọi khi: Admin mở trang, hoặc theo lịch polling
     */
    public Map<String, Object> checkAndCreateAlerts() {
        int newAlerts = 0;

        // 1. Kiểm tra nguyên liệu dưới định mức (LOW_STOCK)
        List<Ingredient> allIngredients = ingredientRepository.findAll();
        for (Ingredient ing : allIngredients) {
            BigDecimal qty = ing.getQuantity() != null ? ing.getQuantity() : BigDecimal.ZERO;
            BigDecimal minStock = ing.getMinStock() != null ? ing.getMinStock() : BigDecimal.ZERO;

            if (qty.signum() <= 0) {
                createNotification("LOW_STOCK",
                        "🚫 Hết hàng: " + ing.getName(),
                        ing.getName() + " đã hết hàng hoàn toàn (tồn kho: 0). Cần nhập bổ sung ngay!",
                        "ROLE_ADMIN", "critical", "ingredient", String.valueOf(ing.getId()));
                newAlerts++;
            } else if (qty.compareTo(minStock) <= 0) {
                createNotification("LOW_STOCK",
                        "⚠️ Sắp hết: " + ing.getName(),
                        ing.getName() + " chỉ còn " + qty + " " + ing.getUnit() +
                                " (định mức tối thiểu: " + minStock + " " + ing.getUnit() + ")",
                        "ROLE_ADMIN", "warning", "ingredient", String.valueOf(ing.getId()));
                newAlerts++;
            }
        }

        // 2. Kiểm tra lô hàng sắp hết hạn (EXPIRING_BATCH)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 3);
        List<IngredientBatch> expiringBatches = ingredientBatchRepository.findExpiringBatches(cal.getTime());
        for (IngredientBatch batch : expiringBatches) {
            long daysLeft = (batch.getExpirationDate().getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
            String daysText = daysLeft <= 0 ? "ĐÃ HẾT HẠN" : "còn " + daysLeft + " ngày";
            createNotification("EXPIRING_BATCH",
                    "📅 Lô sắp hết hạn: " + batch.getIngredient().getName(),
                    "Lô #" + batch.getId() + " của " + batch.getIngredient().getName() +
                            " (" + daysText + "), còn " + batch.getQuantity() + " " +
                            batch.getIngredient().getUnit(),
                    "ROLE_ADMIN", daysLeft <= 0 ? "critical" : "warning",
                    "batch", String.valueOf(batch.getId()));
            newAlerts++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("newAlerts", newAlerts);
        result.put("totalUnread", notificationRepository.countByIsReadFalse());
        return result;
    }
}
