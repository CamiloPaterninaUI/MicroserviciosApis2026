package com.unibague.pagoservice.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.unibague.pagoservice.support.TestJwtFactory;
import com.unibague.pagoservice.support.TestRsaKeys;

/**
 * Configuracion de prueba para pago-service.
 *
 * En pruebas no hay un Keycloak real disponible, por lo que se provee un
 * JwtDecoder basado en una llave RSA generada en memoria (TestRsaKeys) con
 * el emisor de prueba test-auth. En produccion, el autoconfigurador de
 * Spring Boot crea el JwtDecoder a partir de la propiedad issuer-uri.
 */
@Configuration
public class TestSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestRsaKeys.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TestJwtFactory.ISSUER));
        return decoder;
    }
}
