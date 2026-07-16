package poly.edu.quanlynhahang.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {
    @Test
    void rejectsCommonPassword() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> PasswordPolicy.validate("password123"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
    }

    @Test
    void acceptsNonCommonPasswordWithinLengthLimit() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("MocVi-2026-Secure"));
    }
}
