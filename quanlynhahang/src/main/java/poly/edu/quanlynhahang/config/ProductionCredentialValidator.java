package poly.edu.quanlynhahang.config;

import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.repository.AccountRepository;

@Component
@Profile({"prod", "production", "stage", "staging"})
public class ProductionCredentialValidator implements ApplicationRunner {

    private static final Set<String> FORBIDDEN_PASSWORDS = Set.of(
            "123", "admin123", "password", "password123");

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public ProductionCredentialValidator(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<Account> accounts = accountRepository.findAll();
        long invalidHashes = accounts.stream()
                .map(Account::getPassword)
                .filter(password -> !isBcrypt(password))
                .count();
        if (invalidHashes > 0) {
            throw new IllegalStateException(
                    "Production contains " + invalidHashes + " account password(s) that are not BCrypt hashes.");
        }

        accounts.stream()
                .filter(account -> FORBIDDEN_PASSWORDS.stream()
                        .anyMatch(candidate -> passwordEncoder.matches(candidate, account.getPassword())))
                .findFirst()
                .ifPresent(account -> {
                    throw new IllegalStateException(
                            "A weak or demo credential is forbidden for production account: "
                                    + account.getUsername());
                });
    }

    private boolean isBcrypt(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }
}
