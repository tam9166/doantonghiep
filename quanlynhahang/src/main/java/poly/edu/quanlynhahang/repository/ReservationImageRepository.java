package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ReservationImage;

import java.util.List;

@Repository
public interface ReservationImageRepository extends JpaRepository<ReservationImage, Long> {
    List<ReservationImage> findByAreaIdOrderBySortOrderAsc(Integer areaId);
    List<ReservationImage> findByTableIdOrderBySortOrderAsc(Integer tableId);
}
