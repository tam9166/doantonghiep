package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import java.util.List;
import java.util.Date;
import java.math.BigDecimal;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface IngredientBatchRepository extends JpaRepository<IngredientBatch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM IngredientBatch b JOIN FETCH b.ingredient WHERE b.id = :batchId")
    Optional<IngredientBatch> findLockedById(@Param("batchId") Long batchId);

    @Modifying
    @Query("UPDATE IngredientBatch b SET b.status = poly.edu.quanlynhahang.entity.IngredientBatchStatus.EXPIRED "
            + "WHERE b.status = poly.edu.quanlynhahang.entity.IngredientBatchStatus.AVAILABLE "
            + "AND b.expirationDate < :now")
    int markExpired(@Param("now") Date now);
    
    List<IngredientBatch> findByIngredientIdOrderByExpirationDateAsc(Long ingredientId);

    // Lấy các lô hàng của 1 nguyên liệu, ưu tiên lô có hạn sử dụng gần nhất (FEFO - First Expired First Out)
    // Chỉ lấy lô còn số lượng > 0
    @Query("SELECT b FROM IngredientBatch b WHERE b.ingredient = :ingredient AND b.quantity > 0 "
            + "AND (b.expirationDate IS NULL OR b.expirationDate >= CURRENT_TIMESTAMP) "
            + "ORDER BY b.expirationDate ASC")
    List<IngredientBatch> findAvailableBatchesOrderByExpirationAsc(Ingredient ingredient);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM IngredientBatch b WHERE b.ingredient.id = :ingredientId AND b.quantity > 0 "
            + "AND (b.expirationDate IS NULL OR b.expirationDate >= CURRENT_TIMESTAMP) "
            + "ORDER BY b.expirationDate ASC, b.id ASC")
    List<IngredientBatch> findAvailableBatchesForUpdate(@Param("ingredientId") Long ingredientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM IngredientBatch b WHERE b.ingredient.id = :ingredientId "
            + "AND (b.expirationDate IS NULL OR b.expirationDate >= :now) "
            + "ORDER BY b.expirationDate ASC, b.id ASC")
    List<IngredientBatch> findRestorableBatchesForUpdate(@Param("ingredientId") Long ingredientId,
                                                         @Param("now") Date now);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM IngredientBatch b "
            + "WHERE b.ingredient.id = :ingredientId AND b.quantity > 0 "
            + "AND (b.expirationDate IS NULL OR b.expirationDate >= CURRENT_TIMESTAMP)")
    BigDecimal sumAvailableByIngredientId(@Param("ingredientId") Long ingredientId);

    // Lấy các lô hàng sắp hết hạn (còn <= X ngày) và còn số lượng > 0
    @Query("SELECT b FROM IngredientBatch b WHERE b.expirationDate <= :targetDate AND b.quantity > 0 ORDER BY b.expirationDate ASC")
    List<IngredientBatch> findExpiringBatches(Date targetDate);

    @Query("SELECT b FROM IngredientBatch b WHERE b.expirationDate >= :fromDate "
            + "AND b.expirationDate <= :targetDate AND b.quantity > 0 "
            + "AND b.status = poly.edu.quanlynhahang.entity.IngredientBatchStatus.AVAILABLE "
            + "ORDER BY b.expirationDate ASC")
    List<IngredientBatch> findExpiringBatchesBetween(@Param("fromDate") Date fromDate,
                                                      @Param("targetDate") Date targetDate);

    @Query("SELECT COUNT(b) FROM IngredientBatch b WHERE b.expirationDate < :now AND b.quantity > 0")
    long countExpiredAvailableBatches(@Param("now") Date now);

    @Query("SELECT b FROM IngredientBatch b JOIN FETCH b.ingredient "
            + "WHERE b.quantity > 0 ORDER BY b.ingredient.id, b.expirationDate, b.id")
    List<IngredientBatch> findPositiveBatchesWithIngredient();
    
    List<IngredientBatch> findByIngredientOrderByImportDateDesc(Ingredient ingredient);
}
