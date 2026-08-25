package poly.edu.quanlynhahang.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.IngredientBatchDisposal;

public interface IngredientBatchDisposalRepository extends JpaRepository<IngredientBatchDisposal, Long> {
    List<IngredientBatchDisposal> findByBatchIdOrderByDisposalDateDesc(Long batchId);
}
