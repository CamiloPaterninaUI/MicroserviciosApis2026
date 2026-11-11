package com.unibague.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibague.auth.dto.AuthResponse;
import com.unibague.auth.dto.LoginRequest;
import com.unibague.auth.dto.RegisterRequest;
import com.unibague.auth.dto.RegisterResponse;
import com.unibague.auth.service.AuthService;

/**
 * Prueba HTTP de la API de autenticación. auth-service delega en Keycloak, por
 * lo que {@link AuthService} se simula; no se necesitan llaves RSA ni Keycloak
 * real en el alcance de prueba unitaria.
 */
@WebMvcTest(AuthController.class)
@Import(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    @TestConfiguration
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registro_devuelve201() throws Exception {
        RegisterResponse esperado = new RegisterResponse(null, "student", "student@taller.local", "USER");
        when(authService.register(any(RegisterRequest.class))).thenReturn(esperado);

        RegisterRequest req = new RegisterRequest("student", "student@taller.local", "Student123!", "Test", "User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("student"));
    }

    @Test
    void login_devuelve200_conAccessToken() throws Exception {
        AuthResponse token = new AuthResponse("Bearer", "access-token-prueba", null, null, "student", "USER");
        when(authService.login(any(LoginRequest.class))).thenReturn(token);

        LoginRequest req = new LoginRequest("student", "Student123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token-prueba"));
    }

    @Test
    void registro_usuarioDuplicado_devuelve409() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new com.unibague.auth.exception.ConflictException("El nombre de usuario ya está registrado"));

        RegisterRequest req = new RegisterRequest("student", "student@taller.local", "Student123!", "Test", "User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }
}
