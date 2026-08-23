package poly.edu.quanlynhahang.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import poly.edu.quanlynhahang.entity.CancellationRequestStatus;
import poly.edu.quanlynhahang.entity.ReservationCancellationRequest;

public interface ReservationCancellationRequestRepository
        extends JpaRepository<ReservationCancellationRequest, Long> {

    boolean existsByReservationIdAndStatusIn(Long reservationId, Collection<CancellationRequestStatus> statuses);

    boolean existsByRequestCode(String requestCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "reservation", "reservation.area", "reservation.table" })
    @Query("select c from ReservationCancellationRequest c where c.id = :id")
    Optional<ReservationCancellationRequest> findLockedById(@Param("id") Long id);

    @EntityGraph(attributePaths = { "reservation", "reservation.area", "reservation.table" })
    List<ReservationCancellationRequest> findAllByOrderByRequestedAtDesc();
}
