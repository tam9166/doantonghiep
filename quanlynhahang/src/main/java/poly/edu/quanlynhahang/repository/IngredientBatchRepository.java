package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import java.util.List;
import java.util.Date;

@Repository
public interface IngredientBatchRepository extends JpaRepository<IngredientBatch, Long> {
    
    // Lấy các lô hàng của 1 nguyên liệu, ưu tiên lô có hạn sử dụng gần nhất (FEFO - First Expired First Out)
    // Chỉ lấy lô còn số lượng > 0
    @Query("SELECT b FROM IngredientBatch b WHERE b.ingredient = :ingredient AND b.quantity > 0 ORDER BY b.expirationDate ASC")
    List<IngredientBatch> findAvailableBatchesOrderByExpirationAsc(Ingredient ingredient);

    // Lấy các lô hàng sắp hết hạn (còn <= X ngày) và còn số lượng > 0
    @Query("SELECT b FROM IngredientBatch b WHERE b.expirationDate <= :targetDate AND b.quantity > 0 ORDER BY b.expirationDate ASC")
    List<IngredientBatch> findExpiringBatches(Date targetDate);
    
    List<IngredientBatch> findByIngredientOrderByImportDateDesc(Ingredient ingredient);
}
