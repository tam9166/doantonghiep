package poly.edu.quanlynhahang.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Timekeeping;

@Repository
public interface TimekeepingRepository extends JpaRepository<Timekeeping, Long> {
    Optional<Timekeeping> findByAccountUsernameAndWorkDate(String username, LocalDate workDate);
    List<Timekeeping> findByWorkDateBetweenOrderByWorkDateAsc(LocalDate startDate, LocalDate endDate);
    List<Timekeeping> findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(String username, LocalDate startDate, LocalDate endDate);
}