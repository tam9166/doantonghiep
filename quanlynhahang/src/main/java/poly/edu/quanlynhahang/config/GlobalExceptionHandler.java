package poly.edu.quanlynhahang.config;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.ApiErrorResponse;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String reason = exception.getReason() == null ? status.name() : exception.getReason();
        String code = toCode(reason, status);
        return response(status, code, toMessage(reason, code), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Dữ liệu không hợp lệ", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fields.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Dữ liệu không hợp lệ", request, fields);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "PERMISSION_DENIED",
                "Bạn không có quyền thực hiện thao tác này.", request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Yêu cầu xác thực hợp lệ.", request, Map.of());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            EntityNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "Không tìm thấy dữ liệu yêu cầu.", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Dữ liệu xung đột với trạng thái hiện tại.", request, Map.of());
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiErrorResponse> handleConcurrentUpdate(
            Exception exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "Dữ liệu vừa được cập nhật bởi yêu cầu khác. Vui lòng tải lại.", request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Nội dung yêu cầu không hợp lệ.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        String correlationId = correlationId(request);
        log.error("Unhandled request error correlationId={} path={}",
                correlationId, request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Có lỗi hệ thống, vui lòng thử lại sau.", request, Map.of(), correlationId);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {
        return response(status, code, message, request, fieldErrors, correlationId(request));
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors,
            String correlationId) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), status.value(), code, message,
                request.getRequestURI(), fieldErrors, correlationId);
        return ResponseEntity.status(status)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, CORRELATION_HEADER)
                .header(CORRELATION_HEADER, correlationId)
                .body(body);
    }

    private String correlationId(HttpServletRequest request) {
        String supplied = request.getHeader(CORRELATION_HEADER);
        if (supplied != null && supplied.matches("[A-Za-z0-9._-]{8,80}")) return supplied;
        return UUID.randomUUID().toString();
    }

    private String toCode(String reason, HttpStatus status) {
        if (reason != null && reason.matches("[A-Z0-9_]+")) return reason;
        return switch (status) {
            case CONFLICT -> "BUSINESS_CONFLICT";
            case NOT_FOUND -> "NOT_FOUND";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "PERMISSION_DENIED";
            case UNPROCESSABLE_ENTITY, BAD_REQUEST -> "VALIDATION_ERROR";
            default -> "REQUEST_ERROR";
        };
    }

    private String toMessage(String reason, String code) {
        if (reason != null && !reason.matches("[A-Z0-9_]+")) return reason;
        return switch (code) {
            case "PAYMENT_WEBHOOK_INVALID_SIGNATURE" -> "Webhook thanh toán không hợp lệ.";
            case "PAYMENT_AMOUNT_MISMATCH" -> "Số tiền thanh toán không khớp.";
            case "PAYMENT_TRANSACTION_DUPLICATED" -> "Giao dịch thanh toán đã được xử lý.";
            case "PAYMENT_QR_EXPIRED" -> "QR thanh toán đã hết hạn.";
            default -> "Yêu cầu không thể xử lý.";
        };
    }
}
