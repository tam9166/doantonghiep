package poly.edu.quanlynhahang.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import poly.edu.quanlynhahang.entity.InventoryReservation;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    boolean existsByOrderIdAndStatus(Integer orderId, poly.edu.quanlynhahang.entity.InventoryReservationStatus status);
    @Query("select coalesce(sum(r.quantity), 0) from InventoryReservation r "
            + "where r.ingredient.id = :ingredientId and r.status = :status and r.expiresAt > :now")
    BigDecimal sumActiveReservedByIngredientId(@Param("ingredientId") Long ingredientId,
                                               @Param("status") InventoryReservationStatus status,
                                               @Param("now") Date now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InventoryReservation r join fetch r.ingredient "
            + "where r.order.id = :orderId order by r.ingredient.id")
    List<InventoryReservation> findLockedByOrderId(@Param("orderId") Integer orderId);
}
