package poly.edu.quanlynhahang.security;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import poly.edu.quanlynhahang.config.ApiErrorWriter;

class RateLimitingFilterTest {
    @Test
    void aiLimitAppliesPerAuthenticatedAccountAcrossDifferentIps() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn("manager01");
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), jwtUtils, errorWriter());

        MockHttpServletResponse lastResponse = null;
        for (int index = 0; index < 31; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/ai/analytics");
            request.setRemoteAddr("10.0.0." + index);
            request.addHeader("Authorization", "Bearer valid-token");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void regenerateQrHasDedicatedRateLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 6; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "POST", "/api/payments/PAY-ABC/regenerate");
            request.setRemoteAddr("10.10.10.10");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    private ApiErrorWriter errorWriter() {
        return new ApiErrorWriter(new ObjectMapper());
    }
}
