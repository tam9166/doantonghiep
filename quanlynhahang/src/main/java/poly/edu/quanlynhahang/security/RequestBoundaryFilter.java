package poly.edu.quanlynhahang.security;

import java.io.IOException;
import java.util.Set;

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

/** Enforces inexpensive request limits before body parsing reaches controllers. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestBoundaryFilter extends OncePerRequestFilter {

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/json", "multipart/form-data", "application/x-www-form-urlencoded");

    private final ApiErrorWriter apiErrorWriter;

    @Value("${app.request.max-json-payload-bytes:1048576}")
    private long maxJsonPayloadBytes = 1_048_576L;

    @Value("${app.request.max-query-length:4096}")
    private int maxQueryLength = 4_096;

    public RequestBoundaryFilter(ApiErrorWriter apiErrorWriter) {
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/") || !BODY_METHODS.contains(request.getMethod())) {
            if (request.getRequestURI().startsWith("/api/") && queryIsTooLong(request)) {
                apiErrorWriter.write(request, response, HttpStatus.valueOf(414),
                        "QUERY_TOO_LARGE", "Request query exceeds the allowed size.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (queryIsTooLong(request)) {
            apiErrorWriter.write(request, response, HttpStatus.valueOf(414),
                    "QUERY_TOO_LARGE", "Request query exceeds the allowed size.");
            return;
        }

        String contentType = request.getContentType();
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxJsonPayloadBytes && !isMultipart(contentType)) {
            apiErrorWriter.write(request, response, HttpStatus.PAYLOAD_TOO_LARGE,
                    "PAYLOAD_TOO_LARGE", "Request payload exceeds the allowed size.");
            return;
        }
        if ((contentLength > 0 || request.getHeader("Transfer-Encoding") != null)
                && !isAllowedContentType(contentType)) {
            apiErrorWriter.write(request, response, HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "UNSUPPORTED_CONTENT_TYPE", "Request content type is not supported.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isMultipart(String contentType) {
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data");
    }

    private boolean isAllowedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) return false;
        String normalized = contentType.toLowerCase(java.util.Locale.ROOT);
        return ALLOWED_CONTENT_TYPES.stream().anyMatch(normalized::startsWith);
    }

    private boolean queryIsTooLong(HttpServletRequest request) {
        String query = request.getQueryString();
        return query != null && query.length() > maxQueryLength;
    }
}
