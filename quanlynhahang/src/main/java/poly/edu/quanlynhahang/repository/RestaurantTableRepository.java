package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.RestaurantTable;

import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Integer> {
    Optional<RestaurantTable> findByName(String name);
}