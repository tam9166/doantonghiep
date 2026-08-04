package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ReservationReviewRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidPhoneRatingAndImageUrl() {
        ReservationReviewCreateRequest request = new ReservationReviewCreateRequest(
                "MV-1", "invalid", 6, null, null, null, null, null, "file:///private/image", false);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsBoundedOptionalReviewFields() {
        ReservationReviewCreateRequest request = new ReservationReviewCreateRequest(
                "MV-20260801-0001", "0901234567", 5, 4, null, null, null,
                "Tot", "https://images.example/review.jpg", true);

        assertTrue(validator.validate(request).isEmpty());
    }
}
