package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ReservationWaitlist;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface ReservationWaitlistRepository extends JpaRepository<ReservationWaitlist, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from ReservationWaitlist entry where entry.id = :id")
    Optional<ReservationWaitlist> findLockedById(@Param("id") Long id);

    Optional<ReservationWaitlist> findByWaitlistCode(String waitlistCode);

    Optional<ReservationWaitlist> findByWaitlistCodeAndCustomerPhone(String waitlistCode, String customerPhone);

    Optional<ReservationWaitlist> findByLinkedReservationCode(String linkedReservationCode);

    long countByReservationDate(LocalDate reservationDate);

    List<ReservationWaitlist> findAllByOrderByCreatedAtDesc();
}
