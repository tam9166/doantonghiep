package poly.edu.quanlynhahang.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Timekeeping;

@Repository
public interface TimekeepingRepository extends JpaRepository<Timekeeping, Long> {
    Optional<Timekeeping> findByAccountUsernameAndWorkDate(String username, Date workDate);
    List<Timekeeping> findByWorkDateBetweenOrderByWorkDateAsc(Date startDate, Date endDate);
    List<Timekeeping> findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(String username, Date startDate, Date endDate);
}
