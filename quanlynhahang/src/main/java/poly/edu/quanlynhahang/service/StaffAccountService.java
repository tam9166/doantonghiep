package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.CreateStaffRequest;
import poly.edu.quanlynhahang.dto.UpdateStaffRequest;
import poly.edu.quanlynhahang.dto.UpdateCustomerRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.AuthorityRepository;
import poly.edu.quanlynhahang.repository.RoleRepository;
import poly.edu.quanlynhahang.security.PasswordPolicy;

import java.util.Locale;
import java.util.Set;
import java.security.SecureRandom;

@Service
public class StaffAccountService {
    private static final char[] TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "ROLE_ADMIN");
    private static final Set<String> MANAGER_ALLOWED_ROLES = Set.of(
            "ROLE_WAITER", "ROLE_KITCHEN", "ROLE_CASHIER");

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public StaffAccountService(AccountRepository accountRepository,
                               RoleRepository roleRepository,
                               AuthorityRepository authorityRepository,
                               PasswordEncoder passwordEncoder,
                               ActivityLogService activityLogService) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public Account create(CreateStaffRequest request, String requestedRole) {
        Actor actor = currentActor();
        String roleName = normalizeRole(requestedRole);
        assertCanManageRole(actor, roleName);
        Role role = requiredRole(roleName);
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsById(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }

        Account account = new Account();
        account.setUsername(username);
        PasswordPolicy.validate(request.password());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setFullname(request.fullname().trim());
        account.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        account.setShift(trimToNull(request.shift()));
        account.setAssignedArea(trimToNull(request.assignedArea()));
        account.setShiftRate(request.shiftRate());
        account.setEnabled(true);
        account.setTokenVersion(0L);
        account.setMustChangePassword(true);
        Account saved = accountRepository.save(account);
        assignRole(saved, role);
        activityLogService.log("CREATE_STAFF", "Account", username, "Tạo nhân viên với quyền " + roleName);
        return saved;
    }

    @Transactional
    public Account update(String username, UpdateStaffRequest request, String requestedRole) {
        Actor actor = currentActor();
        Account account = lockedAccount(username);
        Set<String> currentRoles = accountRoles(account.getUsername());
        assertCanManageTarget(actor, currentRoles);

        if (request.fullname() != null) account.setFullname(request.fullname().trim());
        if (request.email() != null) account.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        if (request.shift() != null) account.setShift(trimToNull(request.shift()));
        if (request.assignedArea() != null) account.setAssignedArea(trimToNull(request.assignedArea()));
        if (request.shiftRate() != null) account.setShiftRate(request.shiftRate());
        if (request.password() != null && !request.password().isBlank()) {
            PasswordPolicy.validate(request.password());
            account.setPassword(passwordEncoder.encode(request.password()));
            account.setMustChangePassword(true);
            revokeTokens(account);
        }

        if (requestedRole != null && !requestedRole.isBlank()) {
            String nextRole = normalizeRole(requestedRole);
            assertCanManageRole(actor, nextRole);
            if (containsAdmin(currentRoles) && !ADMIN_ROLES.contains(nextRole)) assertNotLastAdmin();
            Role role = requiredRole(nextRole);
            authorityRepository.deleteAll(authorityRepository.findByAccountUsername(account.getUsername()));
            assignRole(account, role);
            revokeTokens(account);
        }

        Account saved = accountRepository.save(account);
        activityLogService.log("UPDATE_STAFF", "Account", account.getUsername(), "Cập nhật hồ sơ/quyền nhân viên");
        return saved;
    }

    @Transactional
    public void disable(String username) {
        Actor actor = currentActor();
        Account account = lockedAccount(username);
        if (actor.username().equalsIgnoreCase(account.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể tự khóa tài khoản đang đăng nhập");
        }
        Set<String> roles = accountRoles(account.getUsername());
        assertCanManageTarget(actor, roles);
        if (containsAdmin(roles)) assertNotLastAdmin();
        account.setEnabled(false);
        revokeTokens(account);
        accountRepository.save(account);
        activityLogService.log("DISABLE_STAFF", "Account", account.getUsername(), "Khóa tài khoản nhân viên");
    }

    @Transactional
    public Account updateCustomer(String username, UpdateCustomerRequest request) {
        currentActor();
        Account account = lockedAccount(username);
        if (!isCustomer(account)) {
            throw new AccessDeniedException("Chỉ được quản lý tài khoản khách hàng");
        }
        if (request.fullname() != null) account.setFullname(request.fullname().trim());
        if (request.email() != null) account.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        if (request.phone() != null) account.setPhone(trimToNull(request.phone()));
        Account saved = accountRepository.save(account);
        activityLogService.log("UPDATE_CUSTOMER", "Account", account.getUsername(), "Cập nhật hồ sơ khách hàng");
        return saved;
    }

    @Transactional
    public String resetCustomerPassword(String username, String newPassword, boolean generateTemporary) {
        currentActor();
        Account account = lockedAccount(username);
        if (!isCustomer(account)) {
            throw new AccessDeniedException("Chỉ được đặt lại mật khẩu khách hàng");
        }
        String temporaryPassword = resetPassword(account, newPassword, generateTemporary);
        activityLogService.log("ADMIN_RESET_PASSWORD", "Account", account.getUsername(), "Đặt lại mật khẩu tài khoản khách hàng; bắt buộc đổi ở lần đăng nhập tiếp theo");
        return temporaryPassword;
    }

    @Transactional
    public String resetStaffPassword(String username, String newPassword, boolean generateTemporary) {
        Actor actor = currentActor();
        if (!actor.admin()) throw new AccessDeniedException("Chỉ Admin được đặt lại mật khẩu nhân viên");
        Account account = lockedAccount(username);
        if (isCustomer(account)) throw new AccessDeniedException("Tài khoản này không phải nhân viên");
        String temporaryPassword = resetPassword(account, newPassword, generateTemporary);
        String roles = String.join(",", accountRoles(account.getUsername()));
        activityLogService.log("ADMIN_RESET_PASSWORD", "Account", account.getUsername(),
                "Đặt lại mật khẩu tài khoản nhân viên (" + roles + "); bắt buộc đổi ở lần đăng nhập tiếp theo");
        return temporaryPassword;
    }

    @Transactional
    public void disableCustomer(String username) {
        setCustomerEnabled(username, false);
    }

    @Transactional
    public void setCustomerEnabled(String username, boolean enabled) {
        currentActor();
        Account account = lockedAccount(username);
        if (!isCustomer(account)) {
            throw new AccessDeniedException("Chỉ được khóa tài khoản khách hàng");
        }
        account.setEnabled(enabled);
        revokeTokens(account);
        accountRepository.save(account);
        activityLogService.log(enabled ? "ENABLE_CUSTOMER" : "DISABLE_CUSTOMER", "Account", account.getUsername(),
                enabled ? "Mở khóa tài khoản khách hàng" : "Khóa tài khoản khách hàng");
    }

    private boolean isCustomer(Account account) {
        Set<String> roles = accountRoles(account.getUsername());
        return roles.isEmpty() || roles.stream().allMatch(role -> "ROLE_USER".equals(role) || "ROLE_CUSTOMER".equals(role));
    }

    private Account lockedAccount(String username) {
        return accountRepository.findLockedByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private void assertCanManageTarget(Actor actor, Set<String> targetRoles) {
        if (actor.admin()) return;
        if (targetRoles.isEmpty()
                || targetRoles.stream().anyMatch(role -> !MANAGER_ALLOWED_ROLES.contains(role))) {
            throw new AccessDeniedException("Manager chỉ được quản lý Waiter, Kitchen và Cashier");
        }
    }

    private void assertCanManageRole(Actor actor, String roleName) {
        if (actor.admin()) return;
        if (!MANAGER_ALLOWED_ROLES.contains(roleName)) {
            throw new AccessDeniedException("Manager không được gán quyền " + roleName);
        }
    }

    private void assertNotLastAdmin() {
        if (authorityRepository.countByRoleNameIn(ADMIN_ROLES) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể khóa hoặc hạ quyền Admin cuối cùng");
        }
    }

    private Role requiredRole(String normalizedRole) {
        return roleRepository.findByNameIgnoreCase(normalizedRole)
                .or(() -> roleRepository.findByNameIgnoreCase(normalizedRole.replace("ROLE_", "")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Vai trò không tồn tại"));
    }

    private void assignRole(Account account, Role role) {
        Authority authority = new Authority();
        authority.setAccount(account);
        authority.setRole(role);
        authorityRepository.save(authority);
    }

    private Set<String> accountRoles(String username) {
        return authorityRepository.findByAccountUsername(username).stream()
                .map(authority -> normalizeRole(authority.getRole().getName()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean containsAdmin(Set<String> roles) {
        return roles.stream().anyMatch(ADMIN_ROLES::contains);
    }

    private String normalizeRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vai trò là bắt buộc");
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private void revokeTokens(Account account) {
        account.setTokenVersion((account.getTokenVersion() == null ? 0L : account.getTokenVersion()) + 1L);
    }

    private String resetPassword(Account account, String requestedPassword, boolean generateTemporary) {
        String password = generateTemporary ? generateTemporaryPassword() : requestedPassword;
        PasswordPolicy.validate(password);
        account.setPassword(passwordEncoder.encode(password));
        account.setMustChangePassword(true);
        revokeTokens(account);
        accountRepository.save(account);
        return generateTemporary ? password : null;
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder("Mv!");
        while (password.length() < 14) {
            password.append(TEMP_PASSWORD_CHARS[SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARS.length)]);
        }
        return password.toString();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Chưa xác thực");
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return new Actor(authentication.getName(), admin);
    }

    private record Actor(String username, boolean admin) {
    }
}
