package poly.edu.quanlynhahang.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {

    /**
     * Stable development-only key. Production is rejected by
     * ApplicationStartupValidator when JWT_SECRET is missing.
     */
    private static final String STABLE_DEV_SECRET =
            "moc-vi-local-development-jwt-key-change-in-production-2026";

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:28800000}")
    private long jwtExpirationMs = 28_800_000L;

    @Value("${app.jwt.issuer:restaurant-api}")
    private String jwtIssuer = "restaurant-api";

    @Value("${app.jwt.audience:restaurant-web}")
    private String jwtAudience = "restaurant-web";

    @Value("${app.jwt.key-id:primary}")
    private String jwtKeyId = "primary";

    private Key signingKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            return Keys.hmacShaKeyFor(STABLE_DEV_SECRET.getBytes(StandardCharsets.UTF_8));
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateJwtToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .setHeaderParam("kid", jwtKeyId)
                .setSubject((userPrincipal.getUsername()))
                .setId(UUID.randomUUID().toString())
                .setIssuer(jwtIssuer)
                .setAudience(jwtAudience)
                .claim("typ", "access")
                .claim("ver", userPrincipal.getTokenVersion())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return claims(token).getSubject();
    }

    public long getTokenVersionFromJwtToken(String token) {
        Number version = claims(token).get("ver", Number.class);
        return version == null ? -1L : version.longValue();
    }

    Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .requireIssuer(jwtIssuer)
                .requireAudience(jwtAudience)
                .require("typ", "access")
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            claims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        }
        return false;
    }
}
