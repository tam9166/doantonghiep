package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.dto.LoginRequest;
import poly.edu.quanlynhahang.security.JwtUtils;

class AuthControllerRoleGateTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerLoginRejectsStaffAccounts() {
        AuthController controller = controllerForRole("ROLE_WAITER");

        assertEquals(HttpStatus.FORBIDDEN, controller.customerLogin(loginRequest()).getStatusCode());
    }

    @Test
    void staffLoginRejectsCustomerAccounts() {
        AuthController controller = controllerForRole("ROLE_USER");

        assertEquals(HttpStatus.FORBIDDEN, controller.staffLogin(loginRequest()).getStatusCode());
    }

    private AuthController controllerForRole(String role) {
        AuthController controller = new AuthController();
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenAnswer(invocation -> List.of(new SimpleGrantedAuthority(role)));
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any())).thenReturn(authentication);
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("test-token");
        ReflectionTestUtils.setField(controller, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(controller, "jwtUtils", jwtUtils);
        return controller;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("test-user");
        request.setPassword("test-password");
        return request;
    }
}
