package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class VoucherCheckRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsBoundedVoucherCodesAndRejectsFreeFormPayloads() {
        assertTrue(validator.validate(new VoucherCheckRequest("WELCOME_10")).isEmpty());
        assertFalse(validator.validate(new VoucherCheckRequest("<script>alert(1)</script>")).isEmpty());
    }
}
