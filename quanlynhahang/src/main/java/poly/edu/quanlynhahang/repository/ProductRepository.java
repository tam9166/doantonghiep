package poly.edu.quanlynhahang.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import poly.edu.quanlynhahang.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Spring Data JPA sẽ tự động hiểu và viết câu lệnh SQL đằng sau
    List<Product> findByCategoryId(Integer categoryId);

    boolean existsByNameIgnoreCase(String name);

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByAvailableTrueAndStatusTrue();

    @Query(value = "SELECT TOP (:limit) od.product_id FROM OrderDetails od "
            + "JOIN Orders o ON o.id = od.order_id "
            + "WHERE od.product_id IS NOT NULL AND (o.is_paid = 1 OR o.payment_status = 'PAID') "
            + "GROUP BY od.product_id ORDER BY SUM(COALESCE(od.quantity, 0)) DESC, od.product_id", nativeQuery = true)
    List<Integer> findTopSellingProductIds(@Param("limit") int limit);
}
