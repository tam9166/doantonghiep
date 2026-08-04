package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ReservationVoucherUsage;

import java.util.List;

@Repository
public interface ReservationVoucherUsageRepository extends JpaRepository<ReservationVoucherUsage, Long> {
    List<ReservationVoucherUsage> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}
