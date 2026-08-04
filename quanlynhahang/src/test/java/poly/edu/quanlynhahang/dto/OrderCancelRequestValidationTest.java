package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class OrderCancelRequestValidationTest {
    @Test void rejectsOversizedCancellationReason() {
        assertFalse(Validation.buildDefaultValidatorFactory().getValidator()
                .validate(new OrderCancelRequest("x".repeat(501))).isEmpty());
    }
}
