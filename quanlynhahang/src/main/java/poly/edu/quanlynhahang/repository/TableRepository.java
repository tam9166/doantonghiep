package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.RestaurantTable;

@Repository
public interface TableRepository extends JpaRepository<RestaurantTable, Integer> {
}