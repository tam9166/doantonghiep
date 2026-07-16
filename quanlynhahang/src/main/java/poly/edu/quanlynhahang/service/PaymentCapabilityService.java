package poly.edu.quanlynhahang.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.config.PaymentProperties;
import poly.edu.quanlynhahang.entity.Reservation;

@Service
public class PaymentCapabilityService {

    public static final String PAYMENT_QR_SCOPE = "PAYMENT_QR";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<String> STAFF_ROLES = Set.of(
            "ROLE_CASHIER", "ROLE_MANAGER", "ROLE_ADMIN");

    private final PaymentProperties paymentProperties;

    public PaymentCapabilityService(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
    }

    public String issue(Reservation reservation, String createdBy) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        reservation.setCreatedBy(createdBy);
        reservation.setPaymentCapabilityTokenHash(hash(rawToken));
        reservation.setPaymentCapabilityScope(PAYMENT_QR_SCOPE);
        reservation.setPaymentCapabilityExpiresAt(Date.from(Instant.now().plusSeconds(
                paymentProperties.getCapabilityExpirationMinutes() * 60L)));
        reservation.setPaymentCapabilityRevoked(false);
        return rawToken;
    }

    public void authorizePaymentQr(Reservation reservation, String rawToken) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isStaff(authentication) || isOwner(authentication, reservation)) {
            return;
        }
        if (rawToken == null || rawToken.isBlank()
                || Boolean.TRUE.equals(reservation.getPaymentCapabilityRevoked())
                || !PAYMENT_QR_SCOPE.equals(reservation.getPaymentCapabilityScope())
                || reservation.getPaymentCapabilityExpiresAt() == null
                || reservation.getPaymentCapabilityExpiresAt().before(new Date())
                || reservation.getPaymentCapabilityTokenHash() == null) {
            throw forbidden();
        }

        byte[] expected = reservation.getPaymentCapabilityTokenHash().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = hash(rawToken).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw forbidden();
        }
    }

    String hash(String rawToken) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean isStaff(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(STAFF_ROLES::contains);
    }

    private boolean isOwner(Authentication authentication, Reservation reservation) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                && reservation.getCreatedBy() != null
                && reservation.getCreatedBy().equals(authentication.getName());
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED");
    }
}
