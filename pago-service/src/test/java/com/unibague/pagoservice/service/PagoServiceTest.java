package com.unibague.pagoservice.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unibague.pagoservice.dto.PagoRequest;
import com.unibague.pagoservice.dto.PagoResponse;
import com.unibague.pagoservice.exception.ResourceNotFoundException;
import com.unibague.pagoservice.model.Pago;
import com.unibague.pagoservice.model.Pago.Estado;
import com.unibague.pagoservice.repository.PagoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PagoService pagoService;

    @Test
    void crearPago_guardaYDevuelveRespuesta() {
        PagoRequest req = new PagoRequest("pedido-1", new BigDecimal("49.90"), "TARJETA");
        when(pagoRepository.existsByPedidoId("pedido-1")).thenReturn(false);
        Pago saved = new Pago();
        saved.setId(1L);
        saved.setPedidoId("pedido-1");
        saved.setMonto(new BigDecimal("49.90"));
        saved.setMetodoPago("TARJETA");
        saved.setEstado(Estado.PENDIENTE);
        saved.setCreadoEn(Instant.now());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PagoResponse res = pagoService.crearPago(req);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.pedidoId()).isEqualTo("pedido-1");
        assertThat(res.monto()).isEqualByComparingTo("49.90");
        assertThat(res.estado()).isEqualTo(Estado.PENDIENTE.name());
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void crearPago_pedidoDuplicado_lanzaExcepcion() {
        PagoRequest req = new PagoRequest("pedido-1", new BigDecimal("49.90"), "TARJETA");
        when(pagoRepository.existsByPedidoId("pedido-1")).thenReturn(true);

        assertThatThrownBy(() -> pagoService.crearPago(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya existe un pago");

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void obtenerPago_noEncontrado_lanzaResourceNotFound() {
        when(pagoRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> pagoService.obtenerPago(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
