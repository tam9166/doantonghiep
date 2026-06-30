package poly.edu.quanlynhahang.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.ServiceZoneAssignment;

@Repository
public interface ServiceZoneAssignmentRepository extends JpaRepository<ServiceZoneAssignment, Long> {

    // Lấy toàn bộ phân công trong 1 ngày
    List<ServiceZoneAssignment> findByWorkDate(Date workDate);

    // Lấy phân công của 1 nhân viên trong 1 ngày
    List<ServiceZoneAssignment> findByAccountUsernameAndWorkDate(String username, Date workDate);

    // Lấy phân công theo ca trong 1 ngày
    List<ServiceZoneAssignment> findByWorkDateAndShift(Date workDate, String shift);

    // Lấy phân công theo khoảng thời gian
    List<ServiceZoneAssignment> findByWorkDateBetweenOrderByWorkDateAsc(Date startDate, Date endDate);

    // Kiểm tra trùng lặp: cùng nhân viên, cùng tầng, cùng ca, cùng ngày
    List<ServiceZoneAssignment> findByAccountUsernameAndFloorAndShiftAndWorkDate(
            String username, String floor, String shift, Date workDate);
}
