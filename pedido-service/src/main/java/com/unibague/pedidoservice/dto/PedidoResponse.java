package com.unibague.pedidoservice.dto;

import java.math.BigDecimal;

/**
 * Salida del pedido: DTO plano, sin exponer la entidad JPA.
 */
public record PedidoResponse(
        Integer id,
        Integer productoId,
        Integer cantidad,
        BigDecimal total
) {}
