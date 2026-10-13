package com.unibague.productoservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unibague.productoservice.dto.ProductoRequest;
import com.unibague.productoservice.dto.ProductoResponse;
import com.unibague.productoservice.exception.ResourceNotFoundException;
import com.unibague.productoservice.model.Producto;
import com.unibague.productoservice.repository.ProductoRepository;

/**
 * Prueba la logica del catalogo compartida por REST y SOAP.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    private ProductoService service;

    private final Producto camiseta = new Producto("Camiseta Selección Colombia", BigDecimal.valueOf(120000), 50);

    @BeforeEach
    void setUp() {
        service = new ProductoService(repository);
        camiseta.setId(1);
    }

    @Test
    void listarDevuelveTodosLosProductosComoDto() {
        when(repository.findAll()).thenReturn(List.of(camiseta));

        List<ProductoResponse> productos = service.listar();

        assertThat(productos).hasSize(1);
        assertThat(productos.get(0).nombre()).isEqualTo("Camiseta Selección Colombia");
        assertThat(productos.get(0).precio()).isEqualByComparingTo("120000");
    }

    @Test
    void buscarPorIdDevuelveElProducto() {
        when(repository.findById(1)).thenReturn(Optional.of(camiseta));

        ProductoResponse response = service.buscarPorId(1);

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.stock()).isEqualTo(50);
    }

    @Test
    void buscarPorIdInexistenteLanza404() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crearPersisteElProductoConPrecioBigDecimal() {
        when(repository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(7);
            return p;
        });

        ProductoResponse response = service.crear(
                new ProductoRequest("Bicicleta BMX", new BigDecimal("450000.50"), 4));

        assertThat(response.id()).isEqualTo(7);
        assertThat(response.precio()).isEqualByComparingTo("450000.50");
        assertThat(response.stock()).isEqualTo(4);
    }

    @Test
    void entidadPorIdUsadaPorElSoapFunciona() {
        when(repository.findById(2)).thenReturn(Optional.of(camiseta));

        Producto entidad = service.entidadPorId(2);

        assertThat(entidad.getNombre()).isEqualTo("Camiseta Selección Colombia");
        assertThat(entidad.getPrecio()).isEqualByComparingTo("120000");
    }
}
