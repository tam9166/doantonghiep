package poly.edu.quanlynhahang.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.AuthorityRepository;
import poly.edu.quanlynhahang.repository.RoleRepository;

/**
 * One-time CLI bootstrap. This bean is never loaded by normal dev, test or prod profiles.
 */
@Service
@Profile("bootstrap-admin")
public class DatabaseInitService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitService.class);

    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.confirm:false}")
    private boolean confirmed;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.fullname:}")
    private String fullname;

    @Value("${app.bootstrap-admin.email:}")
    private String email;

    public DatabaseInitService(RoleRepository roleRepository,
                               AccountRepository accountRepository,
                               AuthorityRepository authorityRepository,
                               PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        validateBootstrapRequest();
        if (accountRepository.existsById(username)) {
            throw new IllegalStateException("Bootstrap account already exists; refusing to modify it.");
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ADMIN");
            return roleRepository.save(role);
        });

        Account admin = new Account();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setFullname(fullname.trim());
        admin.setEmail(email.trim().toLowerCase());
        accountRepository.save(admin);

        Authority authority = new Authority();
        authority.setAccount(admin);
        authority.setRole(adminRole);
        authorityRepository.save(authority);
        log.warn("One-time administrator bootstrap completed. Disable the bootstrap-admin profile now.");
    }

    private void validateBootstrapRequest() {
        if (!confirmed) {
            throw new IllegalStateException("Set APP_BOOTSTRAP_ADMIN_CONFIRM=true for the one-time bootstrap.");
        }
        if (username == null || !username.matches("^[A-Za-z0-9._-]{4,50}$")) {
            throw new IllegalStateException("Bootstrap username must contain 4-50 safe characters.");
        }
        if (password == null || password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("Bootstrap password must contain 12-72 characters.");
        }
        if (fullname == null || fullname.isBlank() || fullname.length() > 100) {
            throw new IllegalStateException("Bootstrap full name is required and must not exceed 100 characters.");
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 100) {
            throw new IllegalStateException("Bootstrap email is invalid.");
        }
    }
}
