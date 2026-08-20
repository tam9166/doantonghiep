package poly.edu.quanlynhahang.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.ApiErrorResponse;
import poly.edu.quanlynhahang.exception.InsufficientInventoryException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responseStatusUsesStableErrorContractAndCorrelationId() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/payments/qr");
        request.addHeader("X-Correlation-Id", "request-12345678");

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("IDEMPOTENCY_CONFLICT", response.getBody().code());
        assertEquals("/api/payments/qr", response.getBody().path());
        assertEquals("request-12345678", response.getBody().correlationId());
    }

    @Test
    void accessDeniedFromServiceReturnsForbiddenInsteadOfInternalError() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/admin/staff/user01");

        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("denied"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("PERMISSION_DENIED", response.getBody().code());
    }

    @Test
    void inventoryShortageReturnsConflictWithIngredientDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/orders/7/add-items");

        ResponseEntity<ApiErrorResponse> response = handler.handleInsufficientInventory(
                new InsufficientInventoryException(Map.of("Thit bo", "required=2.0, available=1.0")), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("INSUFFICIENT_INVENTORY", response.getBody().code());
        assertEquals("required=2.0, available=1.0", response.getBody().fieldErrors().get("Thit bo"));
    }
}
