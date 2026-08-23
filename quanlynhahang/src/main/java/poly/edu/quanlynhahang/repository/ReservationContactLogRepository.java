package poly.edu.quanlynhahang.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.ReservationContactLog;

public interface ReservationContactLogRepository extends JpaRepository<ReservationContactLog, Long> {
    List<ReservationContactLog> findByReservationIdOrderByContactedAtDesc(Long reservationId);
}
