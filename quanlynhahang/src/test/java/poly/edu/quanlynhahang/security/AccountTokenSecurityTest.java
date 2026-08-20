package poly.edu.quanlynhahang.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import poly.edu.quanlynhahang.config.ApiErrorWriter;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountTokenSecurityTest {
    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void usernameDoesNotGrantFallbackAdminRole() {
        Account account = account("admin", 0L);
        account.setAuthorities(List.of());

        assertTrue(new CustomUserDetails(account).getAuthorities().isEmpty());
    }

    @Test
    void authenticationLoadsAuthoritiesAndRolesBeforeTheRepositorySessionCloses() throws Exception {
        Method repositoryMethod = AccountRepository.class.getMethod(
                "findForAuthenticationByUsername", String.class);
        EntityGraph graph = repositoryMethod.getAnnotation(EntityGraph.class);
        assertNotNull(graph);
        assertArrayEquals(new String[]{"authorities", "authorities.role"}, graph.attributePaths());

        Account account = account("admin", 0L);
        Role role = new Role();
        role.setName("ADMIN");
        Authority authority = new Authority();
        authority.setAccount(account);
        authority.setRole(role);
        account.setAuthorities(List.of(authority));

        AccountRepository repository = mock(AccountRepository.class);
        when(repository.findForAuthenticationByUsername("admin")).thenReturn(Optional.of(account));
        CustomUserDetailsService service = new CustomUserDetailsService();
        ReflectionTestUtils.setField(service, "accountRepository", repository);

        var details = service.loadUserByUsername("admin");

        assertTrue(details.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        verify(repository).findForAuthenticationByUsername("admin");
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
        var claims = jwtUtils.claims(token);
        assertEquals("restaurant-api", claims.getIssuer());
        assertEquals("restaurant-web", claims.getAudience());
        assertEquals("access", claims.get("typ", String.class));
        assertFalse(claims.getId().isBlank());
        assertTrue(claims.getExpiration().getTime() - claims.getIssuedAt().getTime() <= 900_000L);

        String header = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
        assertTrue(header.contains("\"kid\":\"primary\""));
    }

    @Test
    void firstLoginTokenCanOnlyCallPasswordChangeEndpoint() throws Exception {
        Account account = account("staff01", 0L);
        account.setMustChangePassword(true);
        CustomUserDetails details = new CustomUserDetails(account);
        JwtUtils jwtUtils = mock(JwtUtils.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        when(jwtUtils.validateJwtToken("a.b.c")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("a.b.c")).thenReturn("staff01");
        when(jwtUtils.getTokenVersionFromJwtToken("a.b.c")).thenReturn(0L);
        when(userDetailsService.loadUserByUsername("staff01")).thenReturn(details);

        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(filter, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(filter, "apiErrorWriter",
                new ApiErrorWriter(new ObjectMapper()));
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("GET", "/api/admin/staff");
        blockedRequest.addHeader("Authorization", "Bearer a.b.c");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        filter.doFilter(blockedRequest, blockedResponse, blockedChain);

        assertEquals(403, blockedResponse.getStatus());
        assertTrue(blockedResponse.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
        assertNull(blockedChain.getRequest());

        MockHttpServletRequest allowedRequest = new MockHttpServletRequest("PUT", "/api/auth/password");
        allowedRequest.addHeader("Authorization", "Bearer a.b.c");
        MockFilterChain allowedChain = new MockFilterChain();
        filter.doFilter(allowedRequest, new MockHttpServletResponse(), allowedChain);
        assertNotNull(allowedChain.getRequest());
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
