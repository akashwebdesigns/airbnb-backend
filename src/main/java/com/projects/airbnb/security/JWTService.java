package com.projects.airbnb.security;


import com.projects.airbnb.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JWTService {

    @Value("${jwt.secret.key}")
    private String jwtSecret;

    //Generate the key
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    //Generate the access token
    public String generateAccessToken(User user){
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email",user.getEmail())
                .claim("roles",user.getAuthorities().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+1000*60*20))//20mins
                .signWith(getSecretKey())
                .compact();
    }

    //Generate the refresh token
    public String generateRefreshToken(User user){
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+ 1000L*60*60*24*30))//30 days
                .signWith(getSecretKey())
                .compact();
    }

    //Get user id from the token
    public Long getUserId(String token){
        Claims payload = Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
        return Long.valueOf(payload.getSubject());
    }
}
