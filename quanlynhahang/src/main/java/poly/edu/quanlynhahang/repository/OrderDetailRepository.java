package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import poly.edu.quanlynhahang.entity.OrderDetail;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    @Query(value = """
            SELECT TOP (:limit)
                   p.id AS productId, p.name AS name, p.image AS image, p.price AS price,
                   SUM(CASE WHEN o.create_date >= DATEADD(day, -7, GETDATE()) THEN COALESCE(od.quantity, 0) ELSE 0 END) AS sold7Days,
                   SUM(CASE WHEN o.create_date >= DATEADD(day, -30, GETDATE()) THEN COALESCE(od.quantity, 0) ELSE 0 END) AS sold30Days,
                   SUM(CASE WHEN o.create_date >= DATEADD(day, -90, GETDATE()) THEN COALESCE(od.quantity, 0) ELSE 0 END) AS sold90Days,
                   CAST(SUM(CASE WHEN o.create_date >= DATEADD(day, -7, GETDATE()) THEN COALESCE(od.quantity, 0) ELSE 0 END) * 0.50
                      + SUM(CASE WHEN o.create_date >= DATEADD(day, -30, GETDATE()) THEN COALESCE(od.quantity, 0) ELSE 0 END) * 0.30
                      + SUM(CASE WHEN o.create_date >= DATEADD(day, -90, GETDATE()) THEN COALESCE(od.quantity, 0) ELSE 0 END) * 0.20 AS float) AS weightedScore
            FROM OrderDetails od
            JOIN Orders o ON o.id = od.order_id
            JOIN Products p ON p.id = od.product_id
            WHERE o.status = 4 AND (o.is_paid = 1 OR o.payment_status = 'PAID')
              AND p.status = 1 AND p.available = 1
              AND o.create_date >= DATEADD(day, -90, GETDATE())
            GROUP BY p.id, p.name, p.image, p.price
            ORDER BY weightedScore DESC, p.id ASC
            """, nativeQuery = true)
    java.util.List<HotMenuItemProjection> findHotMenuItems(@Param("limit") int limit);

    @Query("select case when count(od) > 0 then true else false end from OrderDetail od "
            + "where od.order.account.username = :username "
            + "and od.product.id = :productId "
            + "and od.order.status = 4 "
            + "and od.order.isPaid = true")
    boolean existsCompletedPaidPurchase(@Param("username") String username,
                                        @Param("productId") Integer productId);
}
