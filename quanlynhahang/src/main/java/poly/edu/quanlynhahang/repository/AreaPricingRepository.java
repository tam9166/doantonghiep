package poly.edu.quanlynhahang.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.AreaPricing;

public interface AreaPricingRepository extends JpaRepository<AreaPricing, Integer> {
    Optional<AreaPricing> findByAreaId(Integer areaId);
}
