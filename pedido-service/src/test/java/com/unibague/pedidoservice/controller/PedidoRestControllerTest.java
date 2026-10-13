package com.unibague.pedidoservice.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.unibague.pedidoservice.config.SecurityConfig;
import com.unibague.pedidoservice.dto.PedidoResponse;
import com.unibague.pedidoservice.service.PedidoService;
import com.unibague.pedidoservice.support.TestJwtFactory;
import com.unibague.pedidoservice.support.TestRsaKeys;

/**
 * Matriz de seguridad de pedidos con la cadena de filtros real:
 * 401 sin token, 200/creacion con JWT valido, 400 con parametros invalidos.
 * Se conserva el contrato original (POST con query params).
 */
@WebMvcTest(PedidoRestController.class)
@Import(SecurityConfig.class)
class PedidoRestControllerTest {

    private String userToken;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    @TestConfiguration
    @EnableWebSecurity
    static class WebSecurityTestConfiguration {
    }

    @DynamicPropertySource
    static void rsaKey(DynamicPropertyRegistry registry) throws Exception {
        Path publicPem = Files.createTempFile("taller02-pedido-test-public", ".pem");
        Files.writeString(publicPem, TestRsaKeys.publicPem());
        registry.add("app.jwt.public-key", () -> publicPem.toUri().toString());
    }

    @BeforeEach
    void mintTokens() {
        userToken = TestJwtFactory.validToken(TestRsaKeys.privateKey(),
                "11111111-1111-1111-1111-111111111111", "student", "student@taller.local",
                List.of("ROLE_USER"));
    }

    @Test
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarConTokenDevuelve200() throws Exception {
        when(pedidoService.listarPedidos()).thenReturn(List.of(
                new PedidoResponse(1, 2, 3, new BigDecimal("255000.00"))));

        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(255000.00));
    }

    @Test
    void crearPedidoConTokenYParametrosValidosDevuelve200() throws Exception {
        when(pedidoService.crearPedido(anyInt(), anyInt(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new PedidoResponse(4, 2, 3, new BigDecimal("255000.00")));

        mockMvc.perform(post("/api/pedidos")
                        .param("productoId", "2")
                        .param("cantidad", "3")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.productoId").value(2))
                .andExpect(jsonPath("$.total").value(255000.00));
    }

    @Test
    void crearPedidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .param("productoId", "2")
                        .param("cantidad", "3"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearPedidoConCantidadInvalidaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .param("productoId", "2")
                        .param("cantidad", "0")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void crearPedidoConProductoIdInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .param("productoId", "0")
                        .param("cantidad", "1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }
}
