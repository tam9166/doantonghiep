package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ActivityLog;

import java.util.Date;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByOrderByTimestampDesc();

    List<ActivityLog> findByUsernameOrderByTimestampDesc(String username);

    List<ActivityLog> findByEntityTypeOrderByTimestampDesc(String entityType);

    List<ActivityLog> findByActionOrderByTimestampDesc(String action);

    @Query("SELECT a FROM ActivityLog a WHERE a.timestamp >= :from AND a.timestamp <= :to ORDER BY a.timestamp DESC")
    List<ActivityLog> findByDateRange(Date from, Date to);

    @Query("SELECT a FROM ActivityLog a WHERE " +
           "(:username IS NULL OR a.username = :username) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) " +
           "ORDER BY a.timestamp DESC")
    List<ActivityLog> findFiltered(String username, String action, String entityType);

    @Query("SELECT DISTINCT a.username FROM ActivityLog a ORDER BY a.username")
    List<String> findDistinctUsernames();

    long countByAction(String action);
}
