package poly.edu.quanlynhahang.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {

    private final SecretKey fallbackDevKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private int jwtExpirationMs;

    private Key signingKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            return fallbackDevKey;
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateJwtToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .claim("ver", userPrincipal.getTokenVersion())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public long getTokenVersionFromJwtToken(String token) {
        Number version = Jwts.parserBuilder().setSigningKey(signingKey()).build()
                .parseClaimsJws(token).getBody().get("ver", Number.class);
        return version == null ? -1L : version.longValue();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        }
        return false;
    }
}
