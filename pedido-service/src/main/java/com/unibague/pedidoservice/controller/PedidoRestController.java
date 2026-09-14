package com.unibague.pedidoservice.controller;

import com.unibague.pedidoservice.model.Pedido;
import com.unibague.pedidoservice.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoRestController {

    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public List<Pedido> listar() { return pedidoService.listarPedidos(); }

    @PostMapping
    public Pedido crear(@RequestParam int productoId, @RequestParam int cantidad) {
        return pedidoService.crearPedido(productoId, cantidad);
    }
}