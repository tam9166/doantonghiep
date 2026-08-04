package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.TableArea;

import java.util.List;

@Repository
public interface TableAreaRepository extends JpaRepository<TableArea, Integer> {
    List<TableArea> findByStatusOrderByNameViAsc(String status);
}
