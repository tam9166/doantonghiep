package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.CustomerReservationHistory;

import java.util.List;

@Repository
public interface CustomerReservationHistoryRepository extends JpaRepository<CustomerReservationHistory, String> {
    List<CustomerReservationHistory> findAllByOrderByLastReservationAtDesc();
}
