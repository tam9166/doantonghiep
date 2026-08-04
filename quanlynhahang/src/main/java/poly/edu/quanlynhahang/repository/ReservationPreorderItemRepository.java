package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.ReservationPreorderItem;

import java.util.List;

public interface ReservationPreorderItemRepository extends JpaRepository<ReservationPreorderItem, Long> {
    List<ReservationPreorderItem> findByReservationIdOrderByIdAsc(Long reservationId);
}
