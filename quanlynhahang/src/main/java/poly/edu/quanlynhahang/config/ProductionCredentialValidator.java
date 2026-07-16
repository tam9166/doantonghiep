package poly.edu.quanlynhahang.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.repository.AccountRepository;

@Component
@Profile({"prod", "production"})
public class ProductionCredentialValidator implements ApplicationRunner {

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

        accountRepository.findById("manager")
                .filter(account -> passwordEncoder.matches("123", account.getPassword()))
                .ifPresent(account -> {
                    throw new IllegalStateException("Demo credential manager/123 is forbidden in production.");
                });
    }

    private boolean isBcrypt(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }
}
