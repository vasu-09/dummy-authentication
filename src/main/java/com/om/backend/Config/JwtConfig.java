package com.om.backend.Config;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="jwt")
@Getter @Setter
public class JwtConfig {
    private String alg;              // "RS256"
    private String issuer;
    private String kid;              // current key id
    private String privateKeyPem;    // PKCS#8 private key PEM

    // optional rotation
    private String previousKid;
    private String previousPublicPem; // X.509 PUBLIC KEY PEM (-----BEGIN PUBLIC KEY-----)

    private long accessTtlMin;          // e.g., 15
    private long refreshTtlDays;
}


