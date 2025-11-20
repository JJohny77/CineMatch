package com.cinematch.backend.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;                                      // Για extra claims.
import java.util.Map;

@Component
public class JwtUtil {

    // 🔥 Μυστικό κλειδί (256-bit) – ασφαλές για HS256
    private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 🔥 Πόσο ζει ένα token (π.χ. 24 ώρες)
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    // ---------------------------------------------------------------
    // 1) Generate Token
    // ---------------------------------------------------------------
    public String generateToken(String email, String role) {

        Map<String, Object> claims = new HashMap<>();     // Φτιάχνουμε map για extra claims.
        claims.put("role", role);                         // Βάζουμε το role μέσα στο JWT.

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey)
                .compact();
    }

    // ---------------------------------------------------------------
    // 2) Extract Email (subject)
    // ---------------------------------------------------------------
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // ------------------------------------------------------------------
    // 3) Extract Role
    // ------------------------------------------------------------------
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");     // Παίρνει το claim "role".
    }

    // ---------------------------------------------------------------
    // 4) Check if token is expired
    // ---------------------------------------------------------------
    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    // 5) Validate token
// ---------------------------------------------------------------
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------------
    // 6) Internal method: extract all claims
    // ---------------------------------------------------------------
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    // ---------------------------------------------------------------


}
