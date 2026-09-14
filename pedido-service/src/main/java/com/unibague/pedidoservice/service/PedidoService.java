package com.unibague.pedidoservice.service;

import com.unibague.pedidoservice.dto.ProductoDTO;
import com.unibague.pedidoservice.model.Pedido;
import com.unibague.pedidoservice.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Service
public class PedidoService {

    private static final String PRODUCTO_SERVICE_URL = "http://localhost:8081/api/productos/";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PedidoRepository pedidoRepository;

    public Pedido crearPedido(int productoId, int cantidad) {
        ProductoDTO producto = restTemplate.getForObject(PRODUCTO_SERVICE_URL + productoId, ProductoDTO.class);
        if (producto == null) throw new RuntimeException("Producto no encontrado en producto-service");

        double total = producto.getPrecio() * cantidad;
        Pedido pedido = new Pedido(productoId, cantidad, total);
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() { return pedidoRepository.findAll(); }
}