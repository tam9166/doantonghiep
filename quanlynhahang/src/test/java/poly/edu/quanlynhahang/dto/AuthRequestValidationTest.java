package poly.edu.quanlynhahang.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void signupRejectsUnsafeUsernameWeakPasswordAndInvalidEmail() {
        SignupRequest request = new SignupRequest();
        request.setUsername("bad user");
        request.setPassword("short");
        request.setFullname("");
        request.setEmail("not-an-email");

        Set<String> fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.containsAll(Set.of("username", "password", "fullname", "email")));
    }

    @Test
    void signupRequiresEmailAndAcceptsTheDocumentedContract() {
        SignupRequest missingEmail = validSignup();
        missingEmail.setEmail(" ");
        assertTrue(validator.validate(missingEmail).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));

        SignupRequest malformedEmail = validSignup();
        malformedEmail.setEmail("customer@example");
        assertTrue(validator.validate(malformedEmail).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));

        assertEquals(0, validator.validate(validSignup()).size());
    }

    @Test
    void loginRejectsBlankCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername(" ");
        request.setPassword("");

        Set<String> fields = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.containsAll(Set.of("username", "password")));
    }

    private SignupRequest validSignup() {
        SignupRequest request = new SignupRequest();
        request.setUsername("mocvi.user");
        request.setPassword("MocVi-2026-Secure");
        request.setFullname("Nguyễn Văn A");
        request.setEmail("customer@example.com");
        return request;
    }
}
