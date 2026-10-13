package com.unibague.productoservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unibague.productoservice.dto.ProductoRequest;
import com.unibague.productoservice.dto.ProductoResponse;
import com.unibague.productoservice.exception.ResourceNotFoundException;
import com.unibague.productoservice.model.Producto;
import com.unibague.productoservice.repository.ProductoRepository;

/**
 * Logica del catalogo compartida por el API REST y el endpoint SOAP.
 */
@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(int id) {
        return toResponse(findEntity(id));
    }

    /** Usado por el endpoint SOAP, que necesita la entidad mapeada al WSDL. */
    @Transactional(readOnly = true)
    public Producto entidadPorId(int id) {
        return findEntity(id);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto(request.nombre(), request.precio(), request.stock());
        return toResponse(repository.save(producto));
    }

    private Producto findEntity(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
    }

    private ProductoResponse toResponse(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getPrecio(), p.getStock());
    }
}
