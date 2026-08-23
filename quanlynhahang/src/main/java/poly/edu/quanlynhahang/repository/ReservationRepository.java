package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByReservationCode(String reservationCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.reservationCode = :reservationCode")
    Optional<Reservation> findLockedByReservationCode(@Param("reservationCode") String reservationCode);

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    Optional<Reservation> findByReservationCodeAndCustomerPhone(String reservationCode, String customerPhone);

    @Query("""
            select r from Reservation r
            where (:code is not null and upper(r.reservationCode) = :code)
               or (:phone is not null and r.customerPhone = :phone)
               or (:email is not null and lower(r.customerEmail) = :email)
               or (:name is not null and lower(r.customerName) = :name)
            """)
    List<Reservation> findCancellationVerificationCandidates(
            @Param("code") String code,
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("email") String email);

    Optional<Reservation> findFirstByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String customerEmail);

    long countByReservationDate(LocalDate reservationDate);

    List<Reservation> findByReservationDateAndTableIdAndReservationStatusIn(
            LocalDate reservationDate,
            Integer tableId,
            Collection<ReservationStatus> statuses);

    @Query("""
            select r from Reservation r
            where r.reservationDate = :reservationDate
              and r.table.id = :tableId
              and r.reservationStatus in :statuses
            """)
    List<Reservation> findByTableIdAndReservationDateAndReservationStatusIn(
            @Param("tableId") Integer tableId,
            @Param("reservationDate") LocalDate reservationDate,
            @Param("statuses") Collection<ReservationStatus> statuses);

    List<Reservation> findByReservationDateAndReservationStatusIn(
            LocalDate reservationDate, Collection<ReservationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from Reservation r
            where r.reservationDate = :reservationDate
              and (r.table.id = :tableId or exists (
                    select assignment.id from ReservationTableAssignment assignment
                    where assignment.reservation = r and assignment.table.id = :tableId
              ))
              and r.reservationStatus in :statuses
            """)
    List<Reservation> findLockedByReservationDateAndTableIdAndReservationStatusIn(
            @Param("reservationDate") LocalDate reservationDate,
            @Param("tableId") Integer tableId,
            @Param("statuses") Collection<ReservationStatus> statuses);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
            "area", "table", "tableAssignments", "tableAssignments.table"
    })
    List<Reservation> findAllByOrderByCreatedAtDesc();

    @Query("""
            select r.id from Reservation r
            where (r.depositExpiresAt is not null
                   and r.depositExpiresAt <= :now
                   and r.reservationStatus in :waitingStatuses)
               or (r.createdAt is not null
                   and r.createdAt <= :depositDeadline
                   and r.depositAmount > 0
                   and r.reservationStatus in :legacyDepositStatuses)
               or (r.reservationStatus in :noShowStatuses
                   and r.reservationDate is not null
                   and r.arrivalTime is not null
                   and (r.reservationDate < :noShowDate
                        or (r.reservationDate = :noShowDate
                            and r.arrivalTime <= cast(:noShowTime as LocalTime))))
            order by r.id
            """)
    List<Long> findExpiryCandidateIds(
            @Param("now") Date now,
            @Param("depositDeadline") Date depositDeadline,
            @Param("noShowDate") LocalDate noShowDate,
            @Param("noShowTime") LocalTime noShowTime,
            @Param("waitingStatuses") Collection<ReservationStatus> waitingStatuses,
            @Param("legacyDepositStatuses") Collection<ReservationStatus> legacyDepositStatuses,
            @Param("noShowStatuses") Collection<ReservationStatus> noShowStatuses,
            Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
            "area", "table", "tableAssignments", "tableAssignments.table"
    })
    @Query("select distinct r from Reservation r where r.id in :ids")
    List<Reservation> findExpiryCandidatesByIdIn(@Param("ids") Collection<Long> ids);

    List<Reservation> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    List<Reservation> findByCustomerPhoneOrderByCreatedAtDesc(String customerPhone);

    @Modifying
    @Transactional
    @Query("update Reservation r set r.receiptEmailStatus = :status, r.receiptEmailSentAt = :sentAt, r.receiptEmailError = :error where r.id = :id")
    int updateReceiptDelivery(@Param("id") Long id, @Param("status") String status,
                              @Param("sentAt") java.util.Date sentAt, @Param("error") String error);
}
