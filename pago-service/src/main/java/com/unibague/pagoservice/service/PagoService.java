package com.unibague.pagoservice.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unibague.pagoservice.dto.PagoRequest;
import com.unibague.pagoservice.dto.PagoResponse;
import com.unibague.pagoservice.exception.ResourceNotFoundException;
import com.unibague.pagoservice.model.Pago;
import com.unibague.pagoservice.model.Pago.Estado;
import com.unibague.pagoservice.repository.PagoRepository;

/**
 * Logica de negocio de pagos (decisiones propias del Proyecto 03).
 */
@Service
public class PagoService {

    private final PagoRepository pagoRepository;

    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public PagoResponse crearPago(PagoRequest request) {
        if (pagoRepository.existsByPedidoId(request.pedidoId())) {
            throw new IllegalArgumentException("Ya existe un pago para el pedido " + request.pedidoId());
        }

        Pago pago = new Pago();
        pago.setPedidoId(request.pedidoId());
        pago.setMonto(request.monto());
        pago.setMetodoPago(request.metodoPago());
        pago.setEstado(Estado.PENDIENTE);
        pago.setCreadoEn(Instant.now());

        pago = pagoRepository.save(pago);
        return toResponse(pago);
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPago(Long id) {
        Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + id));
        return toResponse(pago);
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPagoPorPedido(String pedidoId) {
        return pagoRepository.findByPedidoId(pedidoId).stream()
            .findFirst()
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("No hay pago para el pedido " + pedidoId));
    }

    @Transactional
    public PagoResponse actualizarEstado(Long id, Estado estado) {
        Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + id));
        pago.setEstado(estado);
        pago = pagoRepository.save(pago);
        return toResponse(pago);
    }

    private PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
            pago.getId(),
            pago.getPedidoId(),
            pago.getMonto(),
            pago.getMetodoPago(),
            pago.getEstado().name(),
            pago.getCreadoEn()
        );
    }
}
