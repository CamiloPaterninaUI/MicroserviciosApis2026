package com.unibague.productoservice.endpoint;

import com.unibague.productoservice.repository.ProductoRepository;
import com.unibague.productoservice.wsdl.GetProductoRequest;
import com.unibague.productoservice.wsdl.GetProductoResponse;
import com.unibague.productoservice.wsdl.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.*;

@Endpoint
public class ProductoEndpoint {

    private static final String NAMESPACE_URI = "http://unibague.edu.co/productos";

    @Autowired
    private ProductoRepository repository;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getProductoRequest")
    @ResponsePayload
    public GetProductoResponse getProducto(@RequestPayload GetProductoRequest request) {
        GetProductoResponse response = new GetProductoResponse();
        var p = repository.buscarPorId(request.getId());

        Producto producto = new Producto();
        producto.setId(p.getId());
        producto.setNombre(p.getNombre());
        producto.setPrecio(p.getPrecio());
        producto.setStock(p.getStock());

        response.setProducto(producto);
        return response;
    }
}