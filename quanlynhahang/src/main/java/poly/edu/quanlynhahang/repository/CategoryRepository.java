package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import poly.edu.quanlynhahang.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByNameIgnoreCaseOrderByIdAsc(String name);
}
