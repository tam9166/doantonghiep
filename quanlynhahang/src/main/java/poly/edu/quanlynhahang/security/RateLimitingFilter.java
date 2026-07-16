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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final JwtUtils jwtUtils;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled = true;

    public RateLimitingFilter(RateLimitService rateLimitService, JwtUtils jwtUtils) {
        this.rateLimitService = rateLimitService;
        this.jwtUtils = jwtUtils;
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

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","code":"RATE_LIMIT_EXCEEDED","message":"Bạn thao tác quá nhanh. Vui lòng thử lại sau.","retryAfterSeconds":%d}
                """.formatted(result.retryAfterSeconds()));
    }

    private RatePolicy resolvePolicy(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equals(method) && path.matches("^/api/auth/(login|staff/login|signup)$")) {
            return new RatePolicy("auth", 10, 60);
        }
        if ("POST".equals(method) && path.equals("/api/reservations")) {
            return new RatePolicy("reservation-create", 20, 60);
        }
        if (path.startsWith("/api/reservations/lookup")) {
            return new RatePolicy("reservation-lookup", 30, 60);
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
        if ("POST".equals(method) && path.startsWith("/api/webhooks/payments")) {
            return new RatePolicy("payment-webhook", 60, 60);
        }
        if ("POST".equals(method) && path.startsWith("/api/applications")) {
            return new RatePolicy("application-upload", 10, 60);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
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
