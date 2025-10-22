package com.sallejoven.backend.config.security;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    // getters/setters
    /** Defaults que se usan si no están en application.yml */
    private String issuer = "salle-joven";
    private Duration accessTtl = Duration.ofMinutes(1);
    private Duration refreshTtl = Duration.ofDays(15);
    private boolean cookieSecure = true;

    private String rsaPrivateKey; // e.g. "classpath:certs/privateKey.pem"
    private String rsaPublicKey;  // e.g. "classpath:certs/publicKey.pem"

}