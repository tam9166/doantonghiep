package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import jakarta.persistence.LockModeType;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {
    boolean existsByOrderId(Integer orderId);
    boolean existsByOrderTableIdAndStatusIn(Integer tableId, Collection<PaymentStatus> statuses);
    Optional<PaymentIntent> findByPaymentCode(String paymentCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentIntent p where p.paymentCode = :paymentCode")
    Optional<PaymentIntent> findLockedByPaymentCode(@Param("paymentCode") String paymentCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentIntent p where p.order.tableId = :tableId order by p.id")
    List<PaymentIntent> findLockedByOrderTableId(@Param("tableId") Integer tableId);
    Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentIntent> findByBankTransactionCode(String bankTransactionCode);
    List<PaymentIntent> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
    List<PaymentIntent> findByReservationIdAndStatusOrderByCreatedAtDesc(Long reservationId, PaymentStatus status);
    Optional<PaymentIntent> findFirstByReservationIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
            Long reservationId, PaymentOption paymentOption, PaymentStatus status);
    Optional<PaymentIntent> findFirstByOrderIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
            Integer orderId, PaymentOption paymentOption, PaymentStatus status);
}
