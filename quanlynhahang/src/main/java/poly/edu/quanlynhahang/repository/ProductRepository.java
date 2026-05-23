package poly.edu.quanlynhahang.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Spring Data JPA sẽ tự động hiểu và viết câu lệnh SQL đằng sau
    List<Product> findByCategoryId(Integer categoryId); 
}