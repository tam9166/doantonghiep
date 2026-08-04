package poly.edu.quanlynhahang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import poly.edu.quanlynhahang.entity.ActivityLog;
import poly.edu.quanlynhahang.repository.ActivityLogRepository;

import java.util.*;
@RestController
@RequestMapping("/api/admin/activity-logs")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
public class ActivityLogController {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    /**
     * Lấy danh sách nhật ký thao tác (có bộ lọc)
     */
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType) {

        List<ActivityLog> logs;
        if (username != null || action != null || entityType != null) {
            logs = activityLogRepository.findFiltered(username, action, entityType);
        } else {
            logs = activityLogRepository.findAllByOrderByTimestampDesc();
        }

        // Giới hạn 500 bản ghi mới nhất
        if (logs.size() > 500) {
            logs = logs.subList(0, 500);
        }

        return ResponseEntity.ok(logs);
    }

    /**
     * Thống kê tổng quan nhật ký
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", activityLogRepository.count());
        stats.put("creates", activityLogRepository.countByAction("CREATE"));
        stats.put("updates", activityLogRepository.countByAction("UPDATE"));
        stats.put("deletes", activityLogRepository.countByAction("DELETE"));
        stats.put("users", activityLogRepository.findDistinctUsernames());
        return ResponseEntity.ok(stats);
    }
}
