package poly.edu.quanlynhahang.dto;

import java.util.Date;

import poly.edu.quanlynhahang.entity.ReservationReview;

/** Internal projection used by administrators for moderation. */
public record ReservationReviewAdminResponse(
        Long id,
        Long reservationId,
        String reservationCode,
        Integer overallRating,
        Integer foodRating,
        Integer serviceRating,
        Integer ambienceRating,
        Integer cleanlinessRating,
        String content,
        String imageUrl,
        Boolean anonymous,
        String adminReply,
        Boolean hidden,
        String hiddenReason,
        Date createdAt,
        Date repliedAt) {

    public static ReservationReviewAdminResponse from(ReservationReview review) {
        return new ReservationReviewAdminResponse(
                review.getId(), review.getReservationId(), review.getReservationCode(), review.getOverallRating(),
                review.getFoodRating(), review.getServiceRating(), review.getAmbienceRating(), review.getCleanlinessRating(),
                review.getContent(), review.getImageUrl(), review.getAnonymous(), review.getAdminReply(), review.getHidden(),
                review.getHiddenReason(), review.getCreatedAt(), review.getRepliedAt());
    }
}
