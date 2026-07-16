package poly.edu.quanlynhahang.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.WheelSpinHistory;

public interface WheelSpinHistoryRepository extends JpaRepository<WheelSpinHistory, Long> {
    Optional<WheelSpinHistory> findByAccountUsernameAndSpinDate(String username, LocalDate spinDate);
}
