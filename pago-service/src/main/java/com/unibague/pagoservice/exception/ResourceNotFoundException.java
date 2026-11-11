package com.unibague.pagoservice.exception;

/**
 * Recurso de pago no encontrado.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
