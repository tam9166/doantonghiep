package poly.edu.quanlynhahang.repository;

import java.util.Date;
import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.quanlynhahang.entity.Order;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    boolean existsByOrderCode(String orderCode);

    List<Order> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(Integer status, LocalDateTime cutoff);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findLockedById(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id in :ids order by o.id")
    List<Order> findLockedByIdIn(@Param("ids") Collection<Integer> ids);

    @Query("select distinct o from Order o "
            + "left join fetch o.restaurantTable t "
            + "left join fetch t.area "
            + "left join fetch o.orderDetails od "
            + "left join fetch od.product")
    List<Order> findAllWithDetails();

    @Query("select o.id from Order o order by o.id desc")
    List<Integer> findRecentOrderIds(Pageable pageable);

    @Query("select distinct o from Order o "
            + "left join fetch o.restaurantTable t left join fetch t.area "
            + "left join fetch o.orderDetails od left join fetch od.product "
            + "where o.id in :ids")
    List<Order> findAllWithDetailsByIdIn(@Param("ids") List<Integer> ids);

    @Query("select distinct o from Order o "
            + "left join fetch o.restaurantTable t left join fetch t.area "
            + "left join fetch o.orderDetails od left join fetch od.product "
            + "where o.status = :status order by o.createDate asc")
    List<Order> findByStatusWithDetails(@Param("status") Integer status);

    @Query("select distinct o from Order o "
            + "left join fetch o.orderDetails od left join fetch od.product "
            + "where o.status = :status and o.createDate >= :startDate "
            + "order by o.createDate asc")
    List<Order> findByStatusSinceWithDetails(@Param("status") Integer status,
                                              @Param("startDate") Date startDate);

    long countByStatus(Integer status);

    @Query("select o from Order o where o.status not in :terminalStatuses and "
            + "((o.scheduledAt is not null and o.scheduledAt < :localCutoff) or "
            + "(o.scheduledAt is null and o.createDate < :dateCutoff)) order by o.id")
    List<Order> findOverdueOrders(@Param("terminalStatuses") Collection<Integer> terminalStatuses,
                                  @Param("localCutoff") LocalDateTime localCutoff,
                                  @Param("dateCutoff") Date dateCutoff);

    @Query("select count(o) from Order o where o.status not in :terminalStatuses and "
            + "((o.scheduledAt is not null and o.scheduledAt < :localCutoff) or "
            + "(o.scheduledAt is null and o.createDate < :dateCutoff))")
    long countOverdueOrders(@Param("terminalStatuses") Collection<Integer> terminalStatuses,
                            @Param("localCutoff") LocalDateTime localCutoff,
                            @Param("dateCutoff") Date dateCutoff);

    @Query("select o from Order o where o.status <> :excludedStatus "
            + "and o.remainingAmount > :minimumRemaining")
    List<Order> findOutstandingOrders(@Param("excludedStatus") Integer excludedStatus,
                                      @Param("minimumRemaining") BigDecimal minimumRemaining);

    @Query("select o from Order o where o.status <> :excludedStatus "
            + "and o.isPaid = true and o.createDate >= :startDate")
    List<Order> findPaidOrdersSince(@Param("excludedStatus") Integer excludedStatus,
                                    @Param("startDate") Date startDate);

    @Query("select distinct o from Order o "
            + "left join fetch o.restaurantTable t left join fetch t.area "
            + "left join fetch o.orderDetails od left join fetch od.product "
            + "where o.status in :activeStatuses "
            + "or (o.status in :completedStatuses and o.createDate >= :startOfDay) "
            + "order by o.createDate asc")
    List<Order> findKitchenBoardOrdersWithDetails(@Param("activeStatuses") List<Integer> activeStatuses,
                                                   @Param("completedStatuses") List<Integer> completedStatuses,
                                                   @Param("startOfDay") Date startOfDay);

    @Query("select distinct o from Order o "
            + "left join fetch o.restaurantTable t "
            + "left join fetch t.area "
            + "left join fetch o.orderDetails od "
            + "left join fetch od.product "
            + "where o.tableId = :tableId "
            + "and (o.isPaid = false or o.isPaid is null) "
            + "and (o.status is null or (o.status <> 3 and o.status <> 4)) "
            + "order by o.createDate desc")
    List<Order> findOpenDineInOrdersByTableIdWithDetails(@Param("tableId") Integer tableId);

    @Query("select distinct o from Order o left join fetch o.orderDetails od "
            + "where o.tableId = :tableId order by o.createDate desc")
    List<Order> findOrdersByTableIdWithDetails(@Param("tableId") Integer tableId);

    @Query("select distinct o from Order o "
            + "left join fetch o.orderDetails od left join fetch od.product "
            + "where o.createDate >= :start and o.createDate < :end "
            + "and (o.status = 4 or o.isPaid = true or o.paymentStatus = :paidStatus)")
    List<Order> findRevenueOrdersBetween(@Param("start") Date start,
                                         @Param("end") Date end,
                                         @Param("paidStatus") PaymentStatus paidStatus);

    // Fetch every relation required by the CRM projection while the query is
    // transactional; this also supports legacy orders with missing table/product.
    @Query("select distinct o from Order o "
            + "left join fetch o.restaurantTable t left join fetch t.area "
            + "left join fetch o.orderDetails od left join fetch od.product "
            + "where o.account.username = :username order by o.createDate desc")
    List<Order> findByAccountUsernameWithDetails(@Param("username") String username);

    List<Order> findByAccountUsername(String username);
    List<Order> findByAddressAndIsPaidFalse(String address);

    @Query("select case when count(o) > 0 then true else false end from Order o "
            + "where o.tableId = :tableId "
            + "and (o.isPaid = false or o.isPaid is null) "
            + "and (o.status is null or o.status <> 3)")
    boolean existsOpenUnpaidOrderForTable(@Param("tableId") Integer tableId);

    @Query("select case when count(o) > 0 then true else false end from Order o "
            + "where o.tableId = :tableId and o.id <> :excludedOrderId "
            + "and (o.status is null or (o.status <> 3 and o.status <> 4))")
    boolean existsActiveOrderForTableExcludingOrder(@Param("tableId") Integer tableId,
                                                     @Param("excludedOrderId") Integer excludedOrderId);

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
