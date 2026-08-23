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
    void publicCheckoutHasDedicatedSpamLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;
        for (int index = 0; index < 11; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders/checkout");
            request.setRemoteAddr("198.51.100.80");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }
        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void eventBookingHasDedicatedSpamLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;
        for (int index = 0; index < 11; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/event-bookings");
            request.setRemoteAddr("198.51.100.81");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }
        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void reservationCancellationCreationHasTightSpamLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;
        for (int index = 0; index < 6; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "POST", "/api/reservation-cancellations");
            request.setRemoteAddr("198.51.100.60");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }
        assertEquals(429, lastResponse.getStatus());
    }
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
    void publicChatbotUsesItsTighterMethodAndPathPolicy() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 11; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/chatbot/chat");
            request.setRemoteAddr("198.51.100.70");
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

    @Test
    void forwardedForHeaderCannotBypassUnauthenticatedAuthRateLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 6; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("198.51.100.10");
            request.addHeader("X-Forwarded-For", "203.0.113." + index);
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void everyApiEndpointUsesTheGlobalFallbackLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 121; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/unmapped-probe");
            request.setRemoteAddr("198.51.100.60");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void legacyReservationCodeLookupIsRateLimited() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 31; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "GET", "/api/reservations/MV-20260729-" + index);
            request.setRemoteAddr("198.51.100.20");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void reservationReviewCreationIsRateLimited() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 21; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservation-reviews");
            request.setRemoteAddr("198.51.100.30");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void reservationWaitlistLookupIsRateLimited() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 31; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "POST", "/api/reservation-waitlist/lookup");
            request.setRemoteAddr("198.51.100.40");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void reservationWaitlistCreationIsRateLimited() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(
                new RateLimitService(), mock(JwtUtils.class), errorWriter());
        MockHttpServletResponse lastResponse = null;

        for (int index = 0; index < 21; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservation-waitlist");
            request.setRemoteAddr("198.51.100.50");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, new MockFilterChain());
        }

        assertEquals(429, lastResponse.getStatus());
    }

    private ApiErrorWriter errorWriter() {
        return new ApiErrorWriter(new ObjectMapper());
    }
}
