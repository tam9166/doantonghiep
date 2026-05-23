package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}