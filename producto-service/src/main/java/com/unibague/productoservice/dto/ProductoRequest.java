package com.unibague.productoservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada para crear o actualizar un producto. Nunca se expone la entidad JPA.
 */
public record ProductoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero") BigDecimal precio,
        @NotNull @Min(value = 0, message = "El stock no puede ser negativo") Integer stock
) {}
