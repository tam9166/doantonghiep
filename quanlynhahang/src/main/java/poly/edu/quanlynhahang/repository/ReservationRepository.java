package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByReservationCode(String reservationCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.reservationCode = :reservationCode")
    Optional<Reservation> findLockedByReservationCode(@Param("reservationCode") String reservationCode);

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    Optional<Reservation> findByReservationCodeAndCustomerPhone(String reservationCode, String customerPhone);

    Optional<Reservation> findFirstByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String customerEmail);

    long countByReservationDate(LocalDate reservationDate);

    List<Reservation> findByReservationDateAndTableIdAndReservationStatusIn(
            LocalDate reservationDate,
            Integer tableId,
            Collection<ReservationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reservation r
            where r.reservationDate = :reservationDate
              and r.table.id = :tableId
              and r.reservationStatus in :statuses
            """)
    List<Reservation> findLockedByReservationDateAndTableIdAndReservationStatusIn(
            @Param("reservationDate") LocalDate reservationDate,
            @Param("tableId") Integer tableId,
            @Param("statuses") Collection<ReservationStatus> statuses);

    List<Reservation> findAllByOrderByCreatedAtDesc();

    List<Reservation> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    List<Reservation> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);
}
