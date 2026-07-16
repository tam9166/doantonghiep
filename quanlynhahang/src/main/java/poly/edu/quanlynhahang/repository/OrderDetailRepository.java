package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import poly.edu.quanlynhahang.entity.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    @Query("select case when count(od) > 0 then true else false end from OrderDetail od "
            + "where od.order.account.username = :username "
            + "and od.product.id = :productId "
            + "and od.order.status = 4 "
            + "and od.order.isPaid = true")
    boolean existsCompletedPaidPurchase(@Param("username") String username,
                                        @Param("productId") Integer productId);
}
