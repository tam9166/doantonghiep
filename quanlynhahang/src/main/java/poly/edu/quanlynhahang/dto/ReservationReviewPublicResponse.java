package poly.edu.quanlynhahang.dto;

import java.util.Date;

import poly.edu.quanlynhahang.entity.ReservationReview;

/** Public projection: deliberately excludes reservation identity and moderation fields. */
public record ReservationReviewPublicResponse(
        Long id,
        Integer overallRating,
        Integer foodRating,
        Integer serviceRating,
        Integer ambienceRating,
        Integer cleanlinessRating,
        String content,
        String imageUrl,
        Boolean anonymous,
        String adminReply,
        Date createdAt,
        Date repliedAt) {

    public static ReservationReviewPublicResponse from(ReservationReview review) {
        return new ReservationReviewPublicResponse(
                review.getId(), review.getOverallRating(), review.getFoodRating(), review.getServiceRating(),
                review.getAmbienceRating(), review.getCleanlinessRating(), review.getContent(), review.getImageUrl(),
                review.getAnonymous(), review.getAdminReply(), review.getCreatedAt(), review.getRepliedAt());
    }
}
