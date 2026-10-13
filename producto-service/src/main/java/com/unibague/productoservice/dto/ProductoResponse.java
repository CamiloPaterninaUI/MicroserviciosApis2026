package com.unibague.productoservice.dto;

import java.math.BigDecimal;

/**
 * Salida del catalogo: DTO plano, sin exponer la entidad JPA.
 */
public record ProductoResponse(
        Integer id,
        String nombre,
        BigDecimal precio,
        Integer stock
) {}
