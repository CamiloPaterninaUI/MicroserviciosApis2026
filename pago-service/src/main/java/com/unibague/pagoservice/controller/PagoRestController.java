package com.unibague.pagoservice.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.unibague.pagoservice.dto.PagoRequest;
import com.unibague.pagoservice.dto.PagoResponse;
import com.unibague.pagoservice.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagos")
@SecurityRequirement(name = "bearerAuth")
public class PagoRestController {

    private final PagoService pagoService;

    public PagoRestController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @Operation(summary = "Crea un pago para un pedido", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagoResponse> crearPago(@Valid @RequestBody PagoRequest request) {
        PagoResponse creado = pagoService.crearPago(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un pago por su ID", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagoResponse> obtenerPago(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.obtenerPago(id));
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Obtiene el pago de un pedido", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PagoResponse> obtenerPagoPorPedido(@PathVariable String pedidoId) {
        return ResponseEntity.ok(pagoService.obtenerPagoPorPedido(pedidoId));
    }

    @PostMapping("/{id}/estado")
    @Operation(summary = "Actualiza el estado de un pago (webhook interno)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> actualizarEstado(@PathVariable Long id,
                                                         @RequestBody EstadoRequest request) {
        return ResponseEntity.ok(pagoService.actualizarEstado(id, com.unibague.pagoservice.model.Pago.Estado.valueOf(request.estado())));
    }

    public record EstadoRequest(String estado) {}
}
