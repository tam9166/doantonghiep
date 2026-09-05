package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ReservationStatusHistory;
import poly.edu.quanlynhahang.entity.ReservationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationStatusHistoryRepository extends JpaRepository<ReservationStatusHistory, Long> {
    List<ReservationStatusHistory> findByReservationIdOrderByChangedAtAsc(Long reservationId);

    Optional<ReservationStatusHistory> findFirstByReservationIdAndNewStatusOrderByChangedAtAsc(
            Long reservationId, ReservationStatus newStatus);
}
