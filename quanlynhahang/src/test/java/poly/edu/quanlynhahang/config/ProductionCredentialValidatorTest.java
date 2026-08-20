package poly.edu.quanlynhahang.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.repository.AccountRepository;

class ProductionCredentialValidatorTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ProductionCredentialValidator validator =
            new ProductionCredentialValidator(accountRepository, passwordEncoder);

    @Test
    void rejectsPlaintextPasswordHash() {
        Account account = account("customer", "plaintext-password");
        when(accountRepository.findAll()).thenReturn(List.of(account));

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void rejectsDemoCredentialForAnyAccount() {
        Account admin = account("admin", passwordEncoder.encode("admin123"));
        when(accountRepository.findAll()).thenReturn(List.of(admin));

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void acceptsStrongBcryptCredentials() {
        Account manager = account("manager", passwordEncoder.encode("Strong-password-2026"));
        when(accountRepository.findAll()).thenReturn(List.of(manager));

        assertDoesNotThrow(() -> validator.run(null));
    }

    private Account account(String username, String password) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(password);
        return account;
    }
}
