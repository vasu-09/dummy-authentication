package com.om.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtKeyConfig {

    @Bean
    public SecretKey hmacKey(JwtConfig cfg) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(cfg.getSecretBase64());
        int minLen = switch (cfg.getAlgorithm()) {
            case "HS256" -> 32; case "HS384" -> 48; case "HS512" -> 64;
            default -> throw new IllegalArgumentException("Unsupported alg: " + cfg.getAlgorithm());
        };
        if (keyBytes.length < minLen) {
            throw new IllegalStateException("JWT key too short for " + cfg.getAlgorithm());
        }
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }

    @Bean
    public io.jsonwebtoken.JwtParser jwtParser(SecretKey hmacKey, JwtConfig cfg) {
        return io.jsonwebtoken.Jwts.parser()
                .verifyWith(hmacKey)
                .requireIssuer(cfg.getIssuer())
                .build();
    }
}

