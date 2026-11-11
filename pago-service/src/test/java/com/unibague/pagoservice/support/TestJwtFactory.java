package com.unibague.pagoservice.support;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Fábrica de JWT de prueba firmados con RS256, equivalente a los que expide
 * Keycloak. Los roles se publican en el claim {@code realm_access.roles}
 * (estándar de Keycloak) y el emisor de prueba es {@code test-auth}.
 */
public final class TestJwtFactory {

    public static final String ISSUER = "test-auth";

    private TestJwtFactory() {}

    public static String validToken(RSAPrivateKey privateKey, String subject, String username,
                                    String email, List<String> roles) {
        return token(privateKey, ISSUER, subject, username, email, roles, Instant.now().plusSeconds(1800));
    }

    public static String token(RSAPrivateKey privateKey, String issuer, String subject, String username,
                               String email, List<String> roles, Instant expiresAt) {
        try {
            List<String> cleanRoles = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .collect(Collectors.toList());
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .claim("username", username)
                    .claim("email", email)
                    .claim("realm_access.roles", cleanRoles)
                    .issueTime(new Date())
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("taller02-key").build(), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible firmar el JWT de prueba", ex);
        }
    }
}
