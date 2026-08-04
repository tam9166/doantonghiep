package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.Notification;
import poly.edu.quanlynhahang.repository.NotificationRepository;
import poly.edu.quanlynhahang.service.NotificationService;

import java.util.*;
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER')")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Lấy tất cả thông báo (mới nhất trước)
     */
    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Notification> notifications = notificationRepository.findAllByOrderByCreatedAtDesc();
        // Giới hạn 100 thông báo gần nhất
        if (notifications.size() > 100) {
            notifications = notifications.subList(0, 100);
        }
        return ResponseEntity.ok(notifications);
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        long count = notificationRepository.countByIsReadFalse();
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    /**
     * Đánh dấu 1 thông báo đã đọc
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Optional<Notification> opt = notificationRepository.findById(id);
        if (opt.isPresent()) {
            Notification n = opt.get();
            n.setIsRead(true);
            notificationRepository.save(n);
            return ResponseEntity.ok("Đã đánh dấu đọc");
        }
        return ResponseEntity.badRequest().body("Không tìm thấy thông báo");
    }

    /**
     * Đánh dấu tất cả đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        notificationRepository.markAllAsRead();
        return ResponseEntity.ok("Đã đánh dấu tất cả đã đọc");
    }

    /**
     * Kiểm tra và tạo cảnh báo mới (gọi bởi frontend polling)
     */
    @PostMapping("/check-alerts")
    public ResponseEntity<?> checkAlerts() {
        Map<String, Object> result = notificationService.checkAndCreateAlerts();
        return ResponseEntity.ok(result);
    }
}
