package com.unibague.pagoservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Contrato de solicitud de pago (decision de diseno propia del Proyecto 03,
 * documentada en docs/ARCHITECTURE.md).
 */
public record PagoRequest(
        @NotBlank(message = "el pedido es obligatorio") String pedidoId,
        @NotNull(message = "el monto es obligatorio")
        @DecimalMin(value = "0.01", inclusive = false, message = "el monto debe ser mayor a cero") BigDecimal monto,
        @NotBlank(message = "el metodo de pago es obligatorio") String metodoPago
) {}
