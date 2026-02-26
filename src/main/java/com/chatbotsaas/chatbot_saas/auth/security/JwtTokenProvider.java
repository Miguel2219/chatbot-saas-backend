package com.chatbotsaas.chatbot_saas.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    // This method takes the user's email, builds a JWT with it, signs it with the secret key and returns the token (creates and signs a JWT from an email)
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email) //sets the "sub" claim
                .issuedAt(new Date()) //sets "iat"
                .expiration(new Date(System.currentTimeMillis() + expiration)) //sets "exp"
                .signWith(getSigningKey()) //signs with your secret
                .compact();
    }

    // parses a token and extracts the subject
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) //uses the same key to verify the signature
                .build()
                .parseSignedClaims(token) //parses and validates the token
                .getPayload()
                .getSubject();
    }

    // returns if the token is valid, false if tampered o expired
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey()) //uses the same key to verify the signature
                    .build()
                    .parseSignedClaims(token) //parses and validates the token
                    .getPayload()
                    .getSubject();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //This method decodes the base64 secret from application.yml and turns it into a key JJWT can use to sign with HMAC-SHA256
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
