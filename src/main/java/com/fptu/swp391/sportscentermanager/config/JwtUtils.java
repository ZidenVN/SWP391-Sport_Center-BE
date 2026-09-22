package com.fptu.swp391.sportscentermanager.config;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final String SECRET = "secret";

    private Key getSigningKey() {
        byte[] bytesKey = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(bytesKey);
    }

    public String generateToken(String username){
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();

    }

    public boolean isTokenValid(String token, String usernameFromUserDetails){
        final String usernameFromToken = extractUsername(token);
        return (usernameFromToken.equals(usernameFromUserDetails) && !isTokenExpired(token));
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
