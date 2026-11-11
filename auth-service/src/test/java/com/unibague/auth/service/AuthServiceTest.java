package com.unibague.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unibague.auth.config.KeycloakClient;
import com.unibague.auth.dto.AuthResponse;
import com.unibague.auth.dto.LoginRequest;
import com.unibague.auth.dto.RegisterRequest;
import com.unibague.auth.dto.RegisterResponse;
import com.unibague.auth.exception.ConflictException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakClient keycloak;

    @InjectMocks
    private AuthService authService;

    @Test
    void registro_creaUsuarioEnKeycloakYDevuelveRespuesta() {
        RegisterRequest req = new RegisterRequest("student", "student@taller.local", "Student123!", "Test", "User");
        RegisterResponse esperado = new RegisterResponse(null, "student", "student@taller.local", "USER");

        RegisterResponse resultado = authService.register(req);

        assertThat(resultado).isEqualTo(esperado);
        verify(keycloak).createUser(req);
    }

    @Test
    void registro_yaExiste_devuelve409() {
        RegisterRequest req = new RegisterRequest("student", "student@taller.local", "Student123!", "Test", "User");
        doThrow(new KeycloakClient.AuthException("user already exists"))
            .when(keycloak).createUser(any(RegisterRequest.class));

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_devuelveTokenDeKeycloak() {
        LoginRequest req = new LoginRequest("student", "Student123!");
        when(keycloak.authenticate("student", "Student123!")).thenReturn("access-token-prueba");

        AuthResponse resultado = authService.login(req);

        assertThat(resultado.accessToken()).isEqualTo("access-token-prueba");
        assertThat(resultado.tokenType()).isEqualTo("Bearer");
        verify(keycloak).authenticate("student", "Student123!");
    }
}
