package com.unibague.productoservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unibague.productoservice.dto.ProductoRequest;
import com.unibague.productoservice.dto.ProductoResponse;
import com.unibague.productoservice.service.ProductoService;
import com.unibague.productoservice.exception.ResourceNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

/**
 * API REST del catalogo. Conserva los endpoints originales
 * (GET lista, GET por id, POST crear) pero con DTOs, validacion
 * y codigos HTTP correctos. La seguridad (JWT + rol ADMIN para crear)
 * se aplica en SecurityConfig.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

    private final ProductoService productoService;

    public ProductoRestController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los productos del catalogo")
    public List<ProductoResponse> listar() {
        return productoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un producto por id (404 si no existe)")
    public ProductoResponse buscarPorId(@PathVariable int id) {
        return productoService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Crea un producto nuevo (solo ADMIN)")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
