package com.unibague.pagoservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibague.pagoservice.dto.PagoRequest;
import com.unibague.pagoservice.dto.PagoResponse;
import com.unibague.pagoservice.exception.ResourceNotFoundException;
import com.unibague.pagoservice.model.Pago;
import com.unibague.pagoservice.model.Pago.Estado;
import com.unibague.pagoservice.service.PagoService;

/**
 * Prueba de rebanada web de PagoService con Spring Boot 4.0.8:
 * {@link WebMvcTest} vive en {@code org.springframework.boot.webmvc.test.autoconfigure}
 * y los beans simulados se declaran con {@link MockitoBean}.
 * No se importa la cadena de seguridad real (Keycloak), por lo que los tests
 * ejercen la lógica del controlador y la validación; la autorización por roles
 * se valida en integración contra Keycloak (Kubernetes).
 */
@WebMvcTest(PagoRestController.class)
@Import(PagoRestControllerTest.TestConfig.class)
class PagoRestControllerTest {

    @TestConfiguration
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagoService pagoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final PagoResponse PAGO = new PagoResponse(
        1L, "pedido-100", new BigDecimal("49.90"), "TARJETA", Estado.APROBADO.name(), Instant.now());

    @Test
    void crearPago_devuelve201() throws Exception {
        when(pagoService.crearPago(any(PagoRequest.class))).thenReturn(PAGO);

        PagoRequest req = new PagoRequest("pedido-100", new BigDecimal("49.90"), "TARJETA");

        mockMvc.perform(post("/api/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pedidoId").value("pedido-100"))
            .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    void obtenerPago_devuelve200() throws Exception {
        when(pagoService.obtenerPago(eq(1L))).thenReturn(PAGO);

        mockMvc.perform(get("/api/pagos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void obtenerPago_noEncontrado_devuelve404() throws Exception {
        when(pagoService.obtenerPago(eq(99L)))
            .thenThrow(new ResourceNotFoundException("Pago no encontrado: 99"));

        mockMvc.perform(get("/api/pagos/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void crearPago_payloadInvalido_devuelve400() throws Exception {
        PagoRequest req = new PagoRequest(null, BigDecimal.ZERO, null);

        mockMvc.perform(post("/api/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}
