package poly.edu.quanlynhahang.security;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CaptchaFilter extends OncePerRequestFilter {

    private final CaptchaVerifier captchaVerifier;

    public CaptchaFilter(CaptchaVerifier captchaVerifier) {
        this.captchaVerifier = captchaVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String action = protectedAction(request);
        if (action == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Captcha-Token");
        if (captchaVerifier.verify(token, clientIp(request), action)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":403,"error":"Forbidden","code":"CAPTCHA_REQUIRED","message":"Vui lòng xác minh CAPTCHA trước khi tiếp tục."}
                """);
    }

    private String protectedAction(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method) && path.matches("^/api/auth/(login|staff/login|signup)$")) {
            return "auth";
        }
        if ("POST".equals(method) && path.equals("/api/reservations")) {
            return "reservation-create";
        }
        if ("POST".equals(method) && path.equals("/api/reservation-waitlist")) {
            return "reservation-waitlist-create";
        }
        if (path.startsWith("/api/chatbot")) {
            return "chatbot";
        }
        if ("POST".equals(method) && path.startsWith("/api/reviews")) {
            return "review-create";
        }
        if ("POST".equals(method) && path.equals("/api/reservation-reviews")) {
            return "reservation-review-create";
        }
        if ("POST".equals(method) && path.startsWith("/api/applications")) {
            return "application-upload";
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        // Do not trust client-provided forwarding headers without explicit proxy support.
        return request.getRemoteAddr();
    }
}
