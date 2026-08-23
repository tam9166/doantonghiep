package poly.edu.quanlynhahang.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import poly.edu.quanlynhahang.entity.TableSession;

import java.util.List;
import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    Optional<TableSession> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TableSession s where s.tableId = :tableId and s.active = true")
    List<TableSession> findActiveByTableIdForUpdate(@Param("tableId") Integer tableId);
}
