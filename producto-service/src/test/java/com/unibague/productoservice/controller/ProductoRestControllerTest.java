package com.unibague.productoservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.unibague.productoservice.config.SecurityConfig;
import com.unibague.productoservice.dto.ProductoRequest;
import com.unibague.productoservice.dto.ProductoResponse;
import com.unibague.productoservice.exception.ResourceNotFoundException;
import com.unibague.productoservice.service.ProductoService;
import com.unibague.productoservice.support.TestJwtFactory;
import com.unibague.productoservice.support.TestRsaKeys;

/**
 * Matriz de seguridad del catalogo (Taller 02): 401 sin token,
 * 403 para ROLE_USER al crear, 201/200/404/400 con tokens RS256 reales.
 * Se prueba la cadena de filtros de seguridad completa.
 */
@WebMvcTest(ProductoRestController.class)
@Import(SecurityConfig.class)
class ProductoRestControllerTest {

    private String userToken;
    private String adminToken;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    /**
     * Igual que en los otros servicios: el slice @WebMvcTest de Boot 4 no
     * importa la autoconfiguracion de seguridad, necesaria para HttpSecurity.
     */
    @TestConfiguration
    @EnableWebSecurity
    static class WebSecurityTestConfiguration {
    }

    @DynamicPropertySource
    static void rsaKey(DynamicPropertyRegistry registry) throws Exception {
        Path publicPem = Files.createTempFile("taller02-producto-test-public", ".pem");
        Files.writeString(publicPem, TestRsaKeys.publicPem());
        registry.add("app.jwt.public-key", () -> publicPem.toUri().toString());
    }

    @BeforeEach
    void mintTokens() {
        userToken = TestJwtFactory.validToken(TestRsaKeys.privateKey(),
                "11111111-1111-1111-1111-111111111111", "student", "student@taller.local",
                List.of("ROLE_USER"));
        adminToken = TestJwtFactory.validToken(TestRsaKeys.privateKey(),
                "22222222-2222-2222-2222-222222222222", "admin", "admin@taller.local",
                List.of("ROLE_ADMIN"));
    }

    @Test
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarConTokenUserDevuelve200() throws Exception {
        when(productoService.listar()).thenReturn(List.of(
                new ProductoResponse(1, "Camiseta Selección Colombia", new BigDecimal("120000.00"), 50)));

        mockMvc.perform(get("/api/productos")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Camiseta Selección Colombia"));
    }

    @Test
    void consultarInexistenteDevuelve404() throws Exception {
        when(productoService.buscarPorId(99)).thenThrow(new ResourceNotFoundException("Producto no encontrado con id 99"));

        mockMvc.perform(get("/api/productos/{id}", 99)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void crearSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Bicicleta\",\"precio\":100,\"stock\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearConTokenUserDevuelve403() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Bicicleta\",\"precio\":100,\"stock\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearConTokenAdminDevuelve201() throws Exception {
        when(productoService.crear(any(ProductoRequest.class))).thenReturn(
                new ProductoResponse(8, "Bicicleta BMX", new BigDecimal("450000.50"), 4));

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Bicicleta BMX\",\"precio\":450000.50,\"stock\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.precio").value(450000.50));
    }

    @Test
    void crearConPrecioInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":-5,\"stock\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.nombre").exists())
                .andExpect(jsonPath("$.validationErrors.precio").exists());
    }
}
