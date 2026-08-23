package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import poly.edu.quanlynhahang.entity.Ingredient;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    @Query("SELECT i FROM Ingredient i WHERE i.quantity < i.minStock")
    List<Ingredient> findLowStockIngredients();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ingredient i where i.id = :id")
    Optional<Ingredient> findLockedById(@Param("id") Long id);
}
