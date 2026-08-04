package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ReservationStatusHistory;

import java.util.List;

@Repository
public interface ReservationStatusHistoryRepository extends JpaRepository<ReservationStatusHistory, Long> {
    List<ReservationStatusHistory> findByReservationIdOrderByChangedAtAsc(Long reservationId);
}
