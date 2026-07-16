package poly.edu.quanlynhahang.config;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import poly.edu.quanlynhahang.dto.ApiErrorResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class ApiErrorWriter {
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      HttpStatus status,
                      String code,
                      String message) throws IOException {
        String correlationId = correlationId(request);
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader("Access-Control-Expose-Headers", CORRELATION_HEADER);
        objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                Instant.now(), status.value(), code, message,
                request.getRequestURI(), Map.of(), correlationId));
    }

    private String correlationId(HttpServletRequest request) {
        String supplied = request.getHeader(CORRELATION_HEADER);
        if (supplied != null && supplied.matches("[A-Za-z0-9._-]{8,80}")) return supplied;
        return UUID.randomUUID().toString();
    }
}
