package com.unibague.productoservice.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.unibague.productoservice.exception.ResourceNotFoundException;
import com.unibague.productoservice.service.ProductoService;
import com.unibague.productoservice.wsdl.GetProductoRequest;
import com.unibague.productoservice.wsdl.GetProductoResponse;
import com.unibague.productoservice.wsdl.Producto;

/**
 * Servicio SOAP original de la tienda ("clasico"), conservado por la
 * evolucion al Taller 02. Delegua en ProductoService para compartir
 * la misma logica y los mismos errores que el API REST.
 */
@Endpoint
public class ProductoEndpoint {

    private static final String NAMESPACE_URI = "http://unibague.edu.co/productos";

    private final ProductoService productoService;

    public ProductoEndpoint(ProductoService productoService) {
        this.productoService = productoService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getProductoRequest")
    @ResponsePayload
    public GetProductoResponse getProducto(@RequestPayload GetProductoRequest request) {
        try {
            var p = productoService.entidadPorId(request.getId());

            Producto producto = new Producto();
            producto.setId(p.getId());
            producto.setNombre(p.getNombre());
            producto.setPrecio(p.getPrecio());
            producto.setStock(p.getStock());

            GetProductoResponse response = new GetProductoResponse();
            response.setProducto(producto);
            return response;
        } catch (ResourceNotFoundException ex) {
            // SOAP no tiene codigos HTTP de negocio: se informa el fault estandar.
            throw ex;
        }
    }
}
