package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class OrderMutationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMalformedGuestBookingAndInvalidDetailIds() {
        assertFalse(validator.validate(new GuestBookingRequest("", "abc", "", "")).isEmpty());
        assertFalse(validator.validate(new SplitTableRequest(1, 2, List.of(0))).isEmpty());
    }

    @Test
    void acceptsBoundedTableMutations() {
        assertTrue(validator.validate(new MergeTablesRequest(1, 2)).isEmpty());
        assertTrue(validator.validate(new SplitTableRequest(1, 2, List.of(10, 11))).isEmpty());
    }
}
