package com.unibague.pagoservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato de respuesta de pago (decision de diseno propia del Proyecto 03).
 */
public record PagoResponse(
        Long id,
        String pedidoId,
        BigDecimal monto,
        String metodoPago,
        String estado,      // PENDIENTE, APROBADO, RECHAZADO
        Instant creadoEn
) {}
