package poly.edu.quanlynhahang.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.security.CustomUserDetailsService;
import poly.edu.quanlynhahang.security.JwtUtils;
import poly.edu.quanlynhahang.service.PaymentCapabilityService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final PaymentProperties paymentProperties = properties();
    private final PaymentCapabilityService capabilityService = new PaymentCapabilityService(paymentProperties);
    private final WebSocketConfig config = new WebSocketConfig(
            mock(JwtUtils.class),
            mock(CustomUserDetailsService.class),
            reservationRepository,
            capabilityService,
            "http://localhost:8080");

    @Test
    void reservationTopicRejectsAnonymousSubscriberWithoutCapability() {
        Reservation reservation = reservation("MV-001", "customer");
        capabilityService.issue(reservation, "customer");
        when(reservationRepository.findByReservationCode("MV-001")).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class,
                () -> config.authorizeSubscription(subscription("/topic/reservations/MV-001", null, null)));
    }

    @Test
    void reservationTopicAllowsMatchingCapabilityOrOwner() {
        Reservation reservation = reservation("MV-001", "customer");
        String capability = capabilityService.issue(reservation, "customer");
        when(reservationRepository.findByReservationCode("MV-001")).thenReturn(Optional.of(reservation));

        assertDoesNotThrow(() -> config.authorizeSubscription(
                subscription("/topic/reservations/MV-001", null, capability)));
        assertDoesNotThrow(() -> config.authorizeSubscription(
                subscription("/topic/reservations/MV-001",
                        new TestingAuthenticationToken("customer", null, "ROLE_CUSTOMER"), null)));
    }

    @Test
    void unknownTopicIsDeniedByDefault() {
        assertThrows(AccessDeniedException.class,
                () -> config.authorizeSubscription(subscription("/topic/internal-data", null, null)));
    }

    @Test
    void adminTopicRequiresAdminOrManager() {
        assertThrows(AccessDeniedException.class,
                () -> config.authorizeSubscription(subscription(
                        "/topic/admin/reservations",
                        new TestingAuthenticationToken("customer", null, "ROLE_CUSTOMER"), null)));
        assertDoesNotThrow(() -> config.authorizeSubscription(subscription(
                "/topic/admin/reservations",
                new TestingAuthenticationToken("manager", null, "ROLE_MANAGER"), null)));
    }

    private StompHeaderAccessor subscription(String destination,
                                             TestingAuthenticationToken authentication,
                                             String capability) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(authentication);
        if (capability != null) {
            accessor.setNativeHeader("X-Reservation-Capability", capability);
        }
        return accessor;
    }

    private Reservation reservation(String code, String owner) {
        Reservation reservation = new Reservation();
        reservation.setReservationCode(code);
        reservation.setCreatedBy(owner);
        return reservation;
    }

    private PaymentProperties properties() {
        PaymentProperties properties = new PaymentProperties();
        properties.setCapabilityExpirationMinutes(30);
        return properties;
    }
}
