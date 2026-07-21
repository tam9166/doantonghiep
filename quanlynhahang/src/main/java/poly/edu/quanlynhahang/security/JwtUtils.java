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

    private final SecretKey fallbackDevKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:900000}")
    private int jwtExpirationMs = 900_000;

    @Value("${app.jwt.issuer:restaurant-api}")
    private String jwtIssuer = "restaurant-api";

    @Value("${app.jwt.audience:restaurant-web}")
    private String jwtAudience = "restaurant-web";

    @Value("${app.jwt.key-id:primary}")
    private String jwtKeyId = "primary";

    private Key signingKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            return fallbackDevKey;
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
