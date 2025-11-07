package com.ead.gearup.service.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpirationMillis;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenDurationMs;

    @Value("${jwt.email_verification.expiration}")
    private long emailVerificationTokenDurationMs;

    /**
     * Generate an access token (short-lived)
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails, new HashMap<>());
    }

    public String generateAccessToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_PUBLIC");

        // Remove "ROLE_" prefix if present for JWT token
        String roleWithoutPrefix = role.startsWith("ROLE_") ? role.substring(5) : role;

        extraClaims.put("role", roleWithoutPrefix);
        extraClaims.put("token_type", "access");
        
        // Add requiresPasswordChange flag if present in extraClaims
        // This will be set by the authentication service

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMillis))
                .signWith(getSignKey())
                .header().add("typ", "JWT")
                .and()
                .compact();
    }

    /**
     * Generate a refresh token (long-lived)
     */
    public String generateRefreshToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_PUBLIC");

        // Remove "ROLE_" prefix if present for JWT token
        String roleWithoutPrefix = role.startsWith("ROLE_") ? role.substring(5) : role;

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", roleWithoutPrefix);
        claims.put("token_type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenDurationMs))
                .signWith(getSignKey())
                .header().add("typ", "JWT")
                .and()
                .compact();
    }

    public String generateEmailVerificationToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "email_verification");

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + emailVerificationTokenDurationMs))
                .signWith(getSignKey())
                .header().add("typ", "JWT")
                .and()
                .compact();
    }

    public String generatePasswordResetToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "password_reset");

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + emailVerificationTokenDurationMs)) // 5 minutes
                .signWith(getSignKey())
                .header().add("typ", "JWT")
                .and()
                .compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validate an access token
     */
    public boolean validateAccessToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        final String type = extractClaim(token, claims -> claims.get("token_type", String.class));
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && "access".equals(type);
    }

    /**
     * Validate a refresh token
     */
    public boolean validateRefreshToken(String refreshToken, UserDetails userDetails) {
        final String username = extractUsername(refreshToken);
        final String type = extractClaim(refreshToken, claims -> claims.get("token_type", String.class));
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(refreshToken)
                && "refresh".equals(type);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getJwtExpirationMillis() {
        return jwtExpirationMillis;
    }

    public long getRefreshTokenDurationMs() {
        return refreshTokenDurationMs;
    }
}
