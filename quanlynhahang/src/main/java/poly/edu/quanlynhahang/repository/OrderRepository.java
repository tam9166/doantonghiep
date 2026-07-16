package poly.edu.quanlynhahang.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Order;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findLockedById(@Param("id") Integer id);

    @Query("select distinct o from Order o "
            + "left join fetch o.orderDetails od "
            + "left join fetch od.product")
    List<Order> findAllWithDetails();

    // Thêm dòng này để tìm danh sách đơn hàng theo username người đặt
    List<Order> findByAccountUsername(String username);
    List<Order> findByAddressAndIsPaidFalse(String address);

    @Query("select case when count(o) > 0 then true else false end from Order o "
            + "where o.account.username = :username "
            + "and o.status = :status "
            + "and o.isPaid = :paid "
            + "and o.totalAmount >= :minimumTotal "
            + "and o.createDate >= :startDate and o.createDate < :endDate")
    boolean existsEligibleLuckyWheelOrder(@Param("username") String username,
                                          @Param("status") Integer status,
                                          @Param("paid") Boolean paid,
                                          @Param("minimumTotal") Double minimumTotal,
                                          @Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate);
}
