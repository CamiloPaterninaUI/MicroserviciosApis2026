package com.unibague.productoservice.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unibague.productoservice.exception.ResourceNotFoundException;
import com.unibague.productoservice.model.Producto;
import com.unibague.productoservice.repository.ProductoRepository;
import com.unibague.productoservice.service.ProductoService;
import com.unibague.productoservice.wsdl.GetProductoRequest;
import com.unibague.productoservice.wsdl.GetProductoResponse;

/**
 * Prueba de compatibilidad SOAP: el endpoint original sigue respondiendo
 * getProductoRequest -> getProductoResponse con el mismo contrato
 * (namespace http://unibague.edu.co/productos) tras la evolucion.
 */
@ExtendWith(MockitoExtension.class)
class ProductoEndpointTest {

    @Mock
    private ProductoRepository repository;

    @Test
    void soapGetProductoDevuelveElProductoDelCatalogo() {
        Producto guayos = new Producto("Guayos Predator", BigDecimal.valueOf(250000), 15);
        guayos.setId(3);
        when(repository.findById(3)).thenReturn(Optional.of(guayos));

        ProductoEndpoint endpoint = new ProductoEndpoint(new ProductoService(repository));

        GetProductoRequest request = new GetProductoRequest();
        request.setId(3);

        GetProductoResponse response = endpoint.getProducto(request);

        assertThat(response.getProducto()).isNotNull();
        assertThat(response.getProducto().getId()).isEqualTo(3);
        assertThat(response.getProducto().getNombre()).isEqualTo("Guayos Predator");
        assertThat(response.getProducto().getPrecio()).isEqualByComparingTo("250000");
        assertThat(response.getProducto().getStock()).isEqualTo(15);
    }

    @Test
    void soapGetProductoInexistenteFallaConErrorDeNegocio() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        ProductoEndpoint endpoint = new ProductoEndpoint(new ProductoService(repository));

        GetProductoRequest request = new GetProductoRequest();
        request.setId(99);

        assertThatThrownBy(() -> endpoint.getProducto(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
