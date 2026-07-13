package poly.edu.quanlynhahang.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query("select distinct o from Order o "
            + "left join fetch o.orderDetails od "
            + "left join fetch od.product")
    List<Order> findAllWithDetails();

    // Thêm dòng này để tìm danh sách đơn hàng theo username người đặt
    List<Order> findByAccountUsername(String username);
    List<Order> findByAddressAndIsPaidFalse(String address);
}
