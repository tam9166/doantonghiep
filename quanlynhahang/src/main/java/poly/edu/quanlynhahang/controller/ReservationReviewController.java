package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationReview;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.dto.ReservationReviewAdminResponse;
import poly.edu.quanlynhahang.dto.ReservationReviewPublicResponse;
import poly.edu.quanlynhahang.dto.ReservationReviewCreateRequest;
import poly.edu.quanlynhahang.dto.ReservationReviewReplyRequest;
import poly.edu.quanlynhahang.dto.ReservationReviewMineRequest;
import poly.edu.quanlynhahang.dto.ReservationReviewVisibilityRequest;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.ReservationReviewRepository;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/reservation-reviews")
public class ReservationReviewController {
    private final ReservationRepository reservationRepository;
    private final ReservationReviewRepository reviewRepository;

    public ReservationReviewController(ReservationRepository reservationRepository,
                                       ReservationReviewRepository reviewRepository) {
        this.reservationRepository = reservationRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/public")
    public ResponseEntity<?> getPublicReviews() {
        return ResponseEntity.ok(Map.of(
                "reviews", reviewRepository.findByHiddenFalseOrderByCreatedAtDesc().stream()
                        .map(ReservationReviewPublicResponse::from).toList(),
                "averageRating", reviewRepository.getVisibleAverageRating() == null ? 0.0 : reviewRepository.getVisibleAverageRating()
        ));
    }

    @PostMapping
    public ResponseEntity<?> createReview(@Valid @RequestBody ReservationReviewCreateRequest request) {
        Reservation reservation = reservationRepository
                .findByReservationCodeAndCustomerPhone(request.reservationCode(), request.customerPhone())
                .orElse(null);
        if (reservation == null) {
            return ResponseEntity.badRequest().body("Không tìm thấy đặt bàn đúng với mã và số điện thoại.");
        }
        if (reservation.getReservationStatus() != ReservationStatus.COMPLETED) {
            return ResponseEntity.badRequest().body("Chỉ có thể đánh giá sau khi đặt bàn đã hoàn tất.");
        }
        if (reviewRepository.existsByReservationId(reservation.getId())) {
            return ResponseEntity.badRequest().body("Đặt bàn này đã được đánh giá.");
        }

        ReservationReview review = new ReservationReview();
        review.setReservationId(reservation.getId());
        review.setReservationCode(reservation.getReservationCode());
        review.setOverallRating(clampRating(request.overallRating()));
        review.setFoodRating(optionalRating(request.foodRating()));
        review.setServiceRating(optionalRating(request.serviceRating()));
        review.setAmbienceRating(optionalRating(request.ambienceRating()));
        review.setCleanlinessRating(optionalRating(request.cleanlinessRating()));
        review.setContent(normalizeOptional(request.content()));
        review.setImageUrl(normalizeOptional(request.imageUrl()));
        review.setAnonymous(Boolean.TRUE.equals(request.anonymous()));
        review.setHidden(false);
        review.setCreatedAt(new Date());
        return ResponseEntity.ok(ReservationReviewPublicResponse.from(reviewRepository.save(review)));
    }

    /**
     * P0-02: Changed from GET path params to POST body to avoid PII in URLs.
     */
    @PostMapping("/mine")
    public ResponseEntity<?> getMyReview(@Valid @RequestBody ReservationReviewMineRequest request) {
        String reservationCode = request.reservationCode();
        String customerPhone = request.customerPhone();
        
        if (reservationCode == null || reservationCode.isBlank() || customerPhone == null || customerPhone.isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập mã đặt bàn và số điện thoại.");
        }
        
        return reservationRepository.findByReservationCodeAndCustomerPhone(reservationCode, customerPhone)
                .flatMap(reservation -> reviewRepository.findByReservationId(reservation.getId()))
                .<ResponseEntity<?>>map(review -> ResponseEntity.ok(ReservationReviewPublicResponse.from(review)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getReviewsForAdmin() {
        return ResponseEntity.ok(reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ReservationReviewAdminResponse::from).toList());
    }

    @PutMapping("/admin/{id}/reply")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> replyReview(@PathVariable Long id, @Valid @RequestBody ReservationReviewReplyRequest payload) {
        return reviewRepository.findById(id).map(review -> {
            review.setAdminReply(payload.adminReply().trim());
            review.setRepliedAt(new Date());
            return ResponseEntity.ok(ReservationReviewAdminResponse.from(reviewRepository.save(review)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/admin/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> updateVisibility(@PathVariable Long id,
                                               @Valid @RequestBody ReservationReviewVisibilityRequest payload) {
        return reviewRepository.findById(id).map(review -> {
            review.setHidden(payload.hidden());
            review.setHiddenReason(normalizeOptional(payload.hiddenReason()));
            return ResponseEntity.ok(ReservationReviewAdminResponse.from(reviewRepository.save(review)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Integer clampRating(Integer rating) {
        if (rating == null) return 5;
        return Math.max(1, Math.min(5, rating));
    }

    private Integer optionalRating(Integer rating) {
        return rating == null ? null : clampRating(rating);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
