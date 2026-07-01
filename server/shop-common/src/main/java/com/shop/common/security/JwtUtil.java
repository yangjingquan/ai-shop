package com.shop.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${shop.jwt.secret}")
    private String secret;

    @Value("${shop.jwt.admin-ttl-hours}")
    private int adminTtlHours;

    @Value("${shop.jwt.merchant-ttl-hours}")
    private int merchantTtlHours;

    @Value("${shop.jwt.wx-ttl-days}")
    private int wxTtlDays;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserType userType, Map<String, Object> claims) {
        long ttlMillis = switch (userType) {
            case ADMIN -> adminTtlHours * 3600_000L;
            case MERCHANT -> merchantTtlHours * 3600_000L;
            case WX -> wxTtlDays * 86400_000L;
        };
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .claim("userType", userType.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
