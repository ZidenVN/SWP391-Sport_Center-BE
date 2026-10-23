package com.fptu.swp391.sportscentermanager.security;


import com.fptu.swp391.sportscentermanager.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private Key getSigningKey() {
        byte[] bytesKey = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(bytesKey);
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("ver", user.currentTokenVersion())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public int extractTokenVersion(String token) {
        Integer ver = Jwts.parser().setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token).getBody().get("ver", Integer.class);
        return ver == null ? 0 : ver;
    }


    public String extractEmail(String token) {
        return Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();

    }

    public boolean isTokenValid(String token, String emailFromUserDetails) {
        final String emailFromToken = extractEmail(token);
        return (emailFromToken.equals(emailFromUserDetails) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getExpiration();
        return expiration.before(new Date());
    }
}
