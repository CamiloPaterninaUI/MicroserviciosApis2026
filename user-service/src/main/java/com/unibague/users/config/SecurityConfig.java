package com.unibague.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server sobre Keycloak (emisor unico JWT). Valida el token
 * mediante descubrimiento OIDC (issuer-uri). Los roles llegan en el claim
 * realm_access.roles y se mapean a autoridades ROLE_*.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health/**"
                ).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<String> roles = jwt.getClaim("realm_access.roles");
            if (roles == null) {
                roles = java.util.List.of();
            }
            return roles.stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                .collect(java.util.stream.Collectors.toList());
        });
        converter.setPrincipalClaimName("sub");
        return converter;
    }
}
