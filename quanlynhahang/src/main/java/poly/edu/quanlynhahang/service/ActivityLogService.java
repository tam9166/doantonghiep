package poly.edu.quanlynhahang.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import poly.edu.quanlynhahang.entity.ActivityLog;
import poly.edu.quanlynhahang.repository.ActivityLogRepository;

import java.util.Date;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    /**
     * Ghi nhật ký thao tác - gọi từ controller sau mỗi hành động CRUD
     */
    public void log(String action, String entityType, String entityId, String description,
                    String oldValue, String newValue) {
        ActivityLog log = new ActivityLog();
        log.setUsername(getCurrentUsername());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setTimestamp(new Date());
        activityLogRepository.save(log);
    }

    /**
     * Ghi log đơn giản (không cần old/new value)
     */
    public void log(String action, String entityType, String entityId, String description) {
        log(action, entityType, entityId, description, null, null);
    }

    /**
     * Lấy username của người đang đăng nhập hiện tại
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
