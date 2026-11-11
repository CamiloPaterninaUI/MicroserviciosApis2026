package com.unibague.auth.service;

import org.springframework.stereotype.Service;

import com.unibague.auth.config.KeycloakClient;
import com.unibague.auth.dto.AuthResponse;
import com.unibague.auth.dto.LoginRequest;
import com.unibague.auth.dto.RegisterRequest;
import com.unibague.auth.dto.RegisterResponse;
import com.unibague.auth.exception.ConflictException;

/**
 * Delegación de identidad a Keycloak (único emisor JWT).
 * - register: crea el usuario en Keycloak mediante la API de administración.
 * - login: obtiene el accessToken de Keycloak (concesión de contraseña OIDC)
 *   y lo devuelve al cliente.
 */
@Service
public class AuthService {

    private final KeycloakClient keycloak;

    public AuthService(KeycloakClient keycloak) {
        this.keycloak = keycloak;
    }

    public RegisterResponse register(RegisterRequest request) {
        try {
            keycloak.createUser(request);
        } catch (KeycloakClient.AuthException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("already exists")) {
                throw new ConflictException("El nombre de usuario ya está registrado");
            }
            throw ex;
        }
        return new RegisterResponse(null, request.username(), request.email(), "USER");
    }

    public AuthResponse login(LoginRequest request) {
        String accessToken = keycloak.authenticate(request.username(), request.password());
        return new AuthResponse("Bearer", accessToken, null, null, request.username(), "USER");
    }
}
