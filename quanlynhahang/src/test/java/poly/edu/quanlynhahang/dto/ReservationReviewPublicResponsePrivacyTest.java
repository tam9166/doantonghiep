package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.ReservationReview;
import tools.jackson.databind.ObjectMapper;

class ReservationReviewPublicResponsePrivacyTest {

    @Test
    void publicResponseDoesNotExposeReservationIdentityOrModerationFields() throws Exception {
        ReservationReview review = new ReservationReview();
        review.setId(9L);
        review.setReservationId(41L);
        review.setReservationCode("MV-PRIVATE-0001");
        review.setOverallRating(5);
        review.setContent("Dich vu tot");
        review.setHidden(true);
        review.setHiddenReason("Internal moderation reason");

        String json = new ObjectMapper().writeValueAsString(ReservationReviewPublicResponse.from(review));

        assertTrue(json.contains("Dich vu tot"));
        assertFalse(json.contains("reservationId"));
        assertFalse(json.contains("MV-PRIVATE-0001"));
        assertFalse(json.contains("hiddenReason"));
        assertFalse(json.contains("\"hidden\""));
    }
}
