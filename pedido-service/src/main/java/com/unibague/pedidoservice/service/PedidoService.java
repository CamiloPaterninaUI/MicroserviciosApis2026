package com.unibague.pedidoservice.service;

import com.unibague.pedidoservice.dto.ProductoDTO;
import com.unibague.pedidoservice.model.Pedido;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PedidoService {

    private static final String PRODUCTO_SERVICE_URL = "http://localhost:8081/api/productos/";

    @Autowired
    private RestTemplate restTemplate;

    private final Map<Integer, Pedido> pedidos = new HashMap<>();
    private final AtomicInteger contador = new AtomicInteger(1);

    public Pedido crearPedido(int productoId, int cantidad) {
        ProductoDTO producto = restTemplate.getForObject(PRODUCTO_SERVICE_URL + productoId, ProductoDTO.class);
        if (producto == null) throw new RuntimeException("Producto no encontrado en producto-service");

        double total = producto.getPrecio() * cantidad;
        int id = contador.getAndIncrement();
        Pedido pedido = new Pedido(id, productoId, cantidad, total);
        pedidos.put(id, pedido);
        return pedido;
    }

    public List<Pedido> listarPedidos() { return new ArrayList<>(pedidos.values()); }
}