package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.Notification;
import poly.edu.quanlynhahang.repository.NotificationRepository;
import poly.edu.quanlynhahang.service.NotificationService;

import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
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
        List<Notification> notifications = notificationRepository.findByTargetRoleInOrderByCreatedAtDesc(currentRoles());
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
        long count = notificationRepository.countUnreadByRoles(currentRoles());
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
            if (!currentRoles().contains(n.getTargetRole())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy thông báo");
            }
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
        notificationRepository.markAllAsReadByRoles(currentRoles());
        return ResponseEntity.ok("Đã đánh dấu tất cả đã đọc");
    }

    /**
     * Kiểm tra và tạo cảnh báo mới (gọi bởi frontend polling)
     */
    @PostMapping("/check-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> checkAlerts() {
        Map<String, Object> result = notificationService.checkAndCreateAlerts();
        return ResponseEntity.ok(result);
    }

    private List<String> currentRoles() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return List.of("ALL");
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        roles.add("ALL");
        authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .forEach(roles::add);
        return List.copyOf(roles);
    }
}
