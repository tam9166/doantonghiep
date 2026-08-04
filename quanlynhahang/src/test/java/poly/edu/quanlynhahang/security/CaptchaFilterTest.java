package poly.edu.quanlynhahang.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CaptchaFilterTest {

    @Test
    void reservationReviewCreationUsesItsDedicatedCaptchaAction() throws Exception {
        CaptchaVerifier verifier = mock(CaptchaVerifier.class);
        when(verifier.verify("captcha-token", "127.0.0.1", "reservation-review-create")).thenReturn(true);
        CaptchaFilter filter = new CaptchaFilter(verifier);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservation-reviews");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Captcha-Token", "captcha-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(verifier).verify("captcha-token", "127.0.0.1", "reservation-review-create");
    }

    @Test
    void waitlistCreationUsesItsDedicatedCaptchaAction() throws Exception {
        CaptchaVerifier verifier = mock(CaptchaVerifier.class);
        when(verifier.verify("captcha-token", "127.0.0.1", "reservation-waitlist-create")).thenReturn(true);
        CaptchaFilter filter = new CaptchaFilter(verifier);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/reservation-waitlist");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Captcha-Token", "captcha-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(verifier).verify("captcha-token", "127.0.0.1", "reservation-waitlist-create");
    }
}
