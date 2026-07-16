package poly.edu.quanlynhahang.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import poly.edu.quanlynhahang.entity.Account;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTokenSecurityTest {
    @Test
    void usernameDoesNotGrantFallbackAdminRole() {
        Account account = account("admin", 0L);
        account.setAuthorities(List.of());

        assertTrue(new CustomUserDetails(account).getAuthorities().isEmpty());
    }

    @Test
    void jwtCarriesCurrentAccountTokenVersion() {
        Account account = account("staff01", 7L);
        CustomUserDetails details = new CustomUserDetails(account);
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 900_000);
        String token = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        assertEquals(7L, jwtUtils.getTokenVersionFromJwtToken(token));
    }

    private Account account(String username, long tokenVersion) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("ignored");
        account.setTokenVersion(tokenVersion);
        account.setEnabled(true);
        return account;
    }
}
