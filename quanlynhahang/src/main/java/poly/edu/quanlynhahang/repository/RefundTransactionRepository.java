package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import poly.edu.quanlynhahang.entity.RefundTransaction;

import java.util.List;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    boolean existsByOrderId(Integer orderId);
    List<RefundTransaction> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
    List<RefundTransaction> findByOrderIdOrderByCreatedAtDesc(Integer orderId);
    List<RefundTransaction> findByStatus(RefundTransaction.RefundStatus status);

    boolean existsByReservationIdAndStatusIn(
            Long reservationId,
            java.util.Collection<RefundTransaction.RefundStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefundTransaction r where r.id = :id")
    java.util.Optional<RefundTransaction> findLockedById(@Param("id") Long id);
}
