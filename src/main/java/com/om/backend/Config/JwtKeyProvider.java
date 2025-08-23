// src/main/java/com/om/backend/Config/JwtKeyProvider.java
package com.om.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyProvider {

    @Bean
    public RSAPrivateKey jwtPrivateKey(JwtConfig cfg) {
        String body = cfg.getPrivateKeyPem()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getMimeDecoder().decode(body);
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid RSA PKCS#8 private key", e);
        }
    }

    // Current public key (derived from private; e = 65537)
    @Bean
    public RSAPublicKey jwtCurrentPublicKey(RSAPrivateKey priv) {
        try {
            var kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(
                    new RSAPublicKeySpec(priv.getModulus(), BigInteger.valueOf(65537)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive RSA public key", e);
        }
    }

    // Optional: previous public key from PEM (if you use rotation)
    @Bean
    public RSAPublicKey jwtPreviousPublicKey(JwtConfig cfg) {
        String pem = cfg.getPreviousPublicPem();
        if (pem == null || pem.isBlank()) return null; // Spring will inject null if requested with @Nullable
        String body = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getMimeDecoder().decode(body);
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid previous RSA PUBLIC KEY PEM", e);
        }
    }
}


