package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.RefundTransaction;

import java.util.List;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    List<RefundTransaction> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
    List<RefundTransaction> findByOrderIdOrderByCreatedAtDesc(Integer orderId);
    List<RefundTransaction> findByStatus(RefundTransaction.RefundStatus status);
}