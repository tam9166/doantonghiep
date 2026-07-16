package poly.edu.quanlynhahang.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.PointsLedger;

public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {
    Optional<PointsLedger> findByEventKey(String eventKey);
}
