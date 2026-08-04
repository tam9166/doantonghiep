package poly.edu.quanlynhahang.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.RestaurantTable;

import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {
    Optional<RestaurantTable> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id = :id")
    Optional<RestaurantTable> findLockedById(@Param("id") Integer id);
}
