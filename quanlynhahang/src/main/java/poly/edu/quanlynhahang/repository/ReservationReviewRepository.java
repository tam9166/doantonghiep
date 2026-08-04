package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import poly.edu.quanlynhahang.entity.ReservationReview;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationReviewRepository extends JpaRepository<ReservationReview, Long> {
    boolean existsByReservationId(Long reservationId);

    Optional<ReservationReview> findByReservationId(Long reservationId);

    List<ReservationReview> findByHiddenFalseOrderByCreatedAtDesc();

    List<ReservationReview> findAllByOrderByCreatedAtDesc();

    @Query("select avg(r.overallRating) from ReservationReview r where r.hidden = false")
    Double getVisibleAverageRating();
}
