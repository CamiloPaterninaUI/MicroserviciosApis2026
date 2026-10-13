package com.unibague.pedidoservice.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unibague.pedidoservice.dto.PedidoResponse;
import com.unibague.pedidoservice.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;

/**
 * API de pedidos. Se conserva el contrato original
 * (POST /api/pedidos?productoId=&cantidad=) sumando validacion.
 */
@RestController
@RequestMapping("/api/pedidos")
@Validated
public class PedidoRestController {

    private final PedidoService pedidoService;

    public PedidoRestController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los pedidos")
    public List<PedidoResponse> listar() {
        return pedidoService.listarPedidos();
    }

    @PostMapping
    @Operation(summary = "Crea un pedido calculando el total contra producto-service (propagando el JWT del usuario)")
    public PedidoResponse crear(@RequestParam @Min(value = 1, message = "El productoId debe ser mayor que cero") int productoId,
                                @RequestParam @Min(value = 1, message = "La cantidad debe ser mayor que cero") int cantidad,
                                @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return pedidoService.crearPedido(productoId, cantidad, authorization);
    }
}
