package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import poly.edu.quanlynhahang.entity.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByIsReadFalseOrderByCreatedAtDesc();

    List<Notification> findByTargetRoleInOrderByCreatedAtDesc(List<String> roles);

    long countByIsReadFalse();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false AND n.targetRole IN :roles")
    long countUnreadByRoles(List<String> roles);

    List<Notification> findByTypeAndRelatedEntityAndRelatedIdAndIsReadFalse(
            String type, String relatedEntity, String relatedId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.isRead = false")
    void markAllAsRead();

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.isRead = false AND n.targetRole IN :roles")
    void markAllAsReadByRoles(List<String> roles);
}
