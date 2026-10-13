package com.unibague.pedidoservice.exception;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(String message) {
        super(message);
    }
}
