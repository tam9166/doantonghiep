package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.CreateStaffRequest;
import poly.edu.quanlynhahang.dto.UpdateStaffRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.AuthorityRepository;
import poly.edu.quanlynhahang.repository.RoleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

class StaffAccountServiceTest {
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final AuthorityRepository authorityRepository = mock(AuthorityRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final StaffAccountService service = new StaffAccountService(
            accountRepository, roleRepository, authorityRepository, passwordEncoder, activityLogService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerCannotCreateAdmin() {
        authenticate("manager01", "ROLE_MANAGER");
        CreateStaffRequest request = new CreateStaffRequest(
                "newadmin", "strong-password", "New Admin", "admin@example.com", null, null, null);

        assertThrows(AccessDeniedException.class, () -> service.create(request, "ROLE_ADMIN"));

        verify(accountRepository, never()).save(any());
        verify(authorityRepository, never()).save(any());
    }

    @Test
    void managerCannotManageAccountWithoutAnAssignedStaffRole() {
        authenticate("manager01", "ROLE_MANAGER");
        Account target = account("orphan", 0L);
        when(accountRepository.findLockedByUsername("orphan")).thenReturn(Optional.of(target));
        when(authorityRepository.findByAccountUsername("orphan")).thenReturn(List.of());

        assertThrows(AccessDeniedException.class,
                () -> service.update("orphan", emptyUpdate(), null));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void adminCreatedStaffMustChangePasswordOnFirstLogin() {
        authenticate("root", "ROLE_ADMIN");
        Role waiter = new Role();
        waiter.setName("ROLE_WAITER");
        CreateStaffRequest request = new CreateStaffRequest(
                "waiter01", "strong-password", "Waiter One", "waiter@example.com", null, null, null);
        when(roleRepository.findByNameIgnoreCase("ROLE_WAITER")).thenReturn(Optional.of(waiter));
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(request, "ROLE_WAITER");

        assertTrue(created.getMustChangePassword());
        verify(authorityRepository).save(any(Authority.class));
    }

    @Test
    void managerCannotDisableOwnAccount() {
        authenticate("manager01", "ROLE_MANAGER");
        Account account = account("manager01", 0L);
        when(accountRepository.findLockedByUsername("manager01")).thenReturn(Optional.of(account));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.disable("manager01"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void lastAdminCannotBeDemoted() {
        authenticate("root", "ROLE_ADMIN");
        Account account = account("admin02", 0L);
        when(accountRepository.findLockedByUsername("admin02")).thenReturn(Optional.of(account));
        when(authorityRepository.findByAccountUsername("admin02"))
                .thenReturn(List.of(authority(account, "ROLE_ADMIN")));
        when(authorityRepository.countByRoleNameIn(any())).thenReturn(1L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update("admin02", emptyUpdate(), "ROLE_WAITER"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(authorityRepository, never()).deleteAll(any());
    }

    @Test
    void roleChangeRevokesExistingTokens() {
        authenticate("root", "ROLE_ADMIN");
        Account account = account("staff01", 4L);
        Role kitchen = new Role();
        kitchen.setName("ROLE_KITCHEN");
        when(accountRepository.findLockedByUsername("staff01")).thenReturn(Optional.of(account));
        when(authorityRepository.findByAccountUsername("staff01"))
                .thenReturn(List.of(authority(account, "ROLE_WAITER")));
        when(roleRepository.findByNameIgnoreCase("ROLE_KITCHEN")).thenReturn(Optional.of(kitchen));
        when(accountRepository.save(account)).thenReturn(account);

        service.update("staff01", emptyUpdate(), "ROLE_KITCHEN");

        assertEquals(5L, account.getTokenVersion());
        verify(authorityRepository).deleteAll(any());
        verify(authorityRepository).save(any(Authority.class));
    }

    @Test
    void adminGeneratedTemporaryPasswordIsHashedAndForcesNextLoginChange() {
        authenticate("root", "ROLE_ADMIN");
        Account account = account("waiter02", 2L);
        when(accountRepository.findLockedByUsername("waiter02")).thenReturn(Optional.of(account));
        when(authorityRepository.findByAccountUsername("waiter02"))
                .thenReturn(List.of(authority(account, "ROLE_WAITER")));
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash-only");

        String temporaryPassword = service.resetStaffPassword("waiter02", null, true);

        assertTrue(temporaryPassword.length() >= 10);
        assertEquals("bcrypt-hash-only", account.getPassword());
        assertTrue(account.getMustChangePassword());
        assertEquals(3L, account.getTokenVersion());
        verify(accountRepository).save(account);
    }

    @Test
    void managerCannotResetStaffPassword() {
        authenticate("manager01", "ROLE_MANAGER");

        assertThrows(AccessDeniedException.class,
                () -> service.resetStaffPassword("waiter02", null, true));

        verify(accountRepository, never()).save(any());
    }

    private UpdateStaffRequest emptyUpdate() {
        return new UpdateStaffRequest(null, null, null, null, null, null);
    }

    private Account account(String username, long tokenVersion) {
        Account account = new Account();
        account.setUsername(username);
        account.setTokenVersion(tokenVersion);
        account.setEnabled(true);
        return account;
    }

    private Authority authority(Account account, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Authority authority = new Authority();
        authority.setAccount(account);
        authority.setRole(role);
        return authority;
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority(role))));
    }
}
