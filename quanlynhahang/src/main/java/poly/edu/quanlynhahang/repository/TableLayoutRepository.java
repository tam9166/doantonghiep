package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.TableLayout;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableLayoutRepository extends JpaRepository<TableLayout, Long> {
    List<TableLayout> findByActiveTrueOrderByFloorNameAscTableIdAsc();

    Optional<TableLayout> findFirstByTableIdAndActiveTrueOrderByUpdatedAtDesc(Integer tableId);
}
