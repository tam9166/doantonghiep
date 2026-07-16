package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.config.PaymentProperties;
import poly.edu.quanlynhahang.entity.Reservation;

class PaymentCapabilityServiceTest {

    private final PaymentCapabilityService service = new PaymentCapabilityService(properties());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storesOnlyHashAndAuthorizesMatchingToken() {
        Reservation reservation = new Reservation();

        String token = service.issue(reservation, null);

        assertFalse(token.isBlank());
        assertNotEquals(token, reservation.getPaymentCapabilityTokenHash());
        assertEquals(64, reservation.getPaymentCapabilityTokenHash().length());
        assertDoesNotThrow(() -> service.authorizePaymentQr(reservation, token));
    }

    @Test
    void tokenForAnotherBillIsRejected() {
        Reservation first = new Reservation();
        Reservation second = new Reservation();
        String firstToken = service.issue(first, null);
        service.issue(second, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.authorizePaymentQr(second, firstToken));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void expiredOrRevokedTokenIsRejected() {
        Reservation reservation = new Reservation();
        String token = service.issue(reservation, null);
        reservation.setPaymentCapabilityExpiresAt(Date.from(Instant.now().minusSeconds(1)));

        assertThrows(ResponseStatusException.class,
                () -> service.authorizePaymentQr(reservation, token));

        reservation.setPaymentCapabilityExpiresAt(Date.from(Instant.now().plusSeconds(60)));
        reservation.setPaymentCapabilityRevoked(true);
        assertThrows(ResponseStatusException.class,
                () -> service.authorizePaymentQr(reservation, token));
    }

    @Test
    void ownerAndAuthorizedStaffDoNotNeedCapabilityToken() {
        Reservation reservation = new Reservation();
        reservation.setCreatedBy("customer");

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("customer", null, "ROLE_CUSTOMER"));
        assertDoesNotThrow(() -> service.authorizePaymentQr(reservation, null));

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("cashier", null, "ROLE_CASHIER"));
        assertDoesNotThrow(() -> service.authorizePaymentQr(reservation, null));
    }

    @Test
    void realtimeSubscriptionRequiresOwnerStaffOrMatchingScopedCapability() {
        Reservation reservation = new Reservation();
        reservation.setCreatedBy("customer");
        String token = service.issue(reservation, "customer");

        assertDoesNotThrow(() -> service.authorizeReservationRealtime(reservation, token, null));
        assertThrows(ResponseStatusException.class,
                () -> service.authorizeReservationRealtime(reservation, "wrong-token", null));

        TestingAuthenticationToken owner = new TestingAuthenticationToken(
                "customer", null, "ROLE_CUSTOMER");
        assertDoesNotThrow(() -> service.authorizeReservationRealtime(reservation, null, owner));

        TestingAuthenticationToken stranger = new TestingAuthenticationToken(
                "another-customer", null, "ROLE_CUSTOMER");
        assertThrows(ResponseStatusException.class,
                () -> service.authorizeReservationRealtime(reservation, null, stranger));
    }

    private PaymentProperties properties() {
        PaymentProperties properties = new PaymentProperties();
        properties.setCapabilityExpirationMinutes(30);
        return properties;
    }
}
