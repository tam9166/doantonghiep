package poly.edu.quanlynhahang.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import poly.edu.quanlynhahang.config.SecurityConfig;

class PasswordSecurityTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Test
    void plaintextDatabasePasswordCannotAuthenticate() {
        assertFalse(passwordEncoder.matches("legacy-password", "legacy-password"));
    }

    @Test
    void bcryptPasswordAuthenticatesNormally() {
        String encoded = passwordEncoder.encode("Strong-password-2026");
        assertTrue(passwordEncoder.matches("Strong-password-2026", encoded));
    }
}
