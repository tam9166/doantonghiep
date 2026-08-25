package poly.edu.quanlynhahang.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.WorkSchedule;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    List<WorkSchedule> findByWorkDateBetweenOrderByWorkDateAsc(Date startDate, Date endDate);
    List<WorkSchedule> findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(String username, Date startDate, Date endDate);
    List<WorkSchedule> findByAccountUsernameAndWorkDate(String username, Date workDate);
    List<WorkSchedule> findByAccountUsernameAndWorkDateBetween(String username, Date startDate, Date endDate);
    List<WorkSchedule> findByWorkDateOrderByShiftAsc(Date workDate);
}
