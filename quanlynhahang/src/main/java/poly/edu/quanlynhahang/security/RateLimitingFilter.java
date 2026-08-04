package poly.edu.quanlynhahang.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import poly.edu.quanlynhahang.config.ApiErrorWriter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final JwtUtils jwtUtils;
    private final ApiErrorWriter apiErrorWriter;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled = true;

    @Value("${app.rate-limit.global-limit:120}")
    private int globalLimit = 120;

    @Value("${app.rate-limit.global-window-seconds:60}")
    private long globalWindowSeconds = 60;

    @Value("${app.rate-limit.auth-limit:5}")
    private int authLimit = 5;

    @Value("${app.rate-limit.auth-window-seconds:900}")
    private long authWindowSeconds = 900;

    public RateLimitingFilter(RateLimitService rateLimitService, JwtUtils jwtUtils, ApiErrorWriter apiErrorWriter) {
        this.rateLimitService = rateLimitService;
        this.jwtUtils = jwtUtils;
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RatePolicy policy = resolvePolicy(request);
        if (!enabled || policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        RateLimitService.RateLimitResult result = rateLimitService.consume(
                policy.name + ":ip:" + ip, policy.limit, policy.windowSeconds);
        String account = authenticatedAccount(request);
        if (result.allowed() && account != null) {
            result = rateLimitService.consume(
                    policy.name + ":account:" + account, policy.limit, policy.windowSeconds);
        }
        if (result.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        apiErrorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMIT_EXCEEDED", "Bạn thao tác quá nhanh. Vui lòng thử lại sau.");
    }

    private RatePolicy resolvePolicy(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method) && path.matches("^/api/auth/(login|staff/login|signup)$")) {
            return new RatePolicy("auth", authLimit, authWindowSeconds);
        }
        if ("POST".equals(method) && path.equals("/api/reservations")) {
            return new RatePolicy("reservation-create", 20, 60);
        }
        if ("POST".equals(method) && path.equals("/api/reservation-waitlist")) {
            return new RatePolicy("reservation-waitlist-create", 20, 60);
        }
        if (path.startsWith("/api/reservations/lookup")) {
            return new RatePolicy("reservation-lookup", 30, 60);
        }
        if ("GET".equals(method) && path.matches("^/api/reservations/[^/]+$")) {
            return new RatePolicy("reservation-lookup", 30, 60);
        }
        if ("GET".equals(method) && path.matches("^/api/reservation-waitlist/[^/]+$")) {
            return new RatePolicy("reservation-waitlist-lookup", 30, 60);
        }
        if ("POST".equals(method) && path.equals("/api/payments/qr")) {
            return new RatePolicy("payment-qr-create", 10, 60);
        }
        if ("POST".equals(method) && path.matches("^/api/payments/[^/]+/regenerate$")) {
            return new RatePolicy("payment-qr-regenerate", 5, 60);
        }
        if ("GET".equals(method) && path.matches("^/api/payments/[^/]+$")) {
            return new RatePolicy("payment-qr-status", 30, 60);
        }
        if (path.startsWith("/api/chatbot")
                || path.startsWith("/api/admin/ai")
                || path.startsWith("/api/staff/ai")) {
            return new RatePolicy("chatbot", 30, 60);
        }
        if ("POST".equals(method) && path.startsWith("/api/reviews")) {
            return new RatePolicy("review-create", 20, 60);
        }
        if ("POST".equals(method) && path.equals("/api/reservation-reviews")) {
            return new RatePolicy("reservation-review-create", 20, 60);
        }
        if ("POST".equals(method) && path.startsWith("/api/webhooks/payments")) {
            return new RatePolicy("payment-webhook", 60, 60);
        }
        if ("POST".equals(method) && path.startsWith("/api/applications")) {
            return new RatePolicy("application-upload", 10, 60);
        }
        if (path.startsWith("/api/")) {
            return new RatePolicy("api", globalLimit, globalWindowSeconds);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        // Forwarded headers are attacker-controlled unless a trusted proxy normalizes them.
        return request.getRemoteAddr();
    }

    private String authenticatedAccount(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7);
        if (!jwtUtils.validateJwtToken(token)) return null;
        return jwtUtils.getUserNameFromJwtToken(token);
    }

    private record RatePolicy(String name, int limit, long windowSeconds) {
    }
}
