package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ReservationWaitlist;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationWaitlistRepository extends JpaRepository<ReservationWaitlist, Long> {
    Optional<ReservationWaitlist> findByWaitlistCode(String waitlistCode);

    Optional<ReservationWaitlist> findByWaitlistCodeAndCustomerPhone(String waitlistCode, String customerPhone);

    long countByReservationDate(LocalDate reservationDate);

    List<ReservationWaitlist> findAllByOrderByCreatedAtDesc();
}
