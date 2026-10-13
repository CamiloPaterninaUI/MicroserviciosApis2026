package com.unibague.pedidoservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.ConnectException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.unibague.pedidoservice.dto.PedidoResponse;
import com.unibague.pedidoservice.dto.ProductoDTO;
import com.unibague.pedidoservice.exception.ProductoNoEncontradoException;
import com.unibague.pedidoservice.exception.ServicioProductoNoDisponibleException;
import com.unibague.pedidoservice.model.Pedido;
import com.unibague.pedidoservice.repository.PedidoRepository;

/**
 * Prueba la creacion de pedidos: calculo monetario exacto con BigDecimal,
 * URL configurable, propagacion del JWT del usuario a producto-service y
 * traduccion de fallos HTTP a errores de negocio (404 / 502).
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final String BEARER = "Bearer eyJhbGciOiJSUzI1NiJ9.jwt-de-prueba";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PedidoRepository pedidoRepository;

    private PedidoService service;

    private final String url = "http://producto-service:8083/api/productos/";

    @BeforeEach
    void setUp() {
        service = new PedidoService(restTemplate, pedidoRepository, url);
    }

    private ProductoDTO producto(BigDecimal precio) {
        ProductoDTO dto = new ProductoDTO();
        dto.setId(1);
        dto.setNombre("Balón de fútbol");
        dto.setPrecio(precio);
        dto.setStock(30);
        return dto;
    }

    @Test
    void crearPedidoCalculaElTotalConBigDecimalExacto() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class)))
                .thenReturn(ResponseEntity.ok(producto(new BigDecimal("85000.00"))));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1);
            return p;
        });

        PedidoResponse response = service.crearPedido(1, 3, BEARER);

        assertThat(response.productoId()).isEqualTo(1);
        assertThat(response.cantidad()).isEqualTo(3);
        assertThat(response.total()).isEqualByComparingTo("255000.00");
    }

    @Test
    void crearPedidoConPrecioDecimalMantieneLaExactitud() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class)))
                .thenReturn(ResponseEntity.ok(producto(new BigDecimal("33333.33"))));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(2);
            return p;
        });

        // Con double este calculo daba 99999.99000000001; BigDecimal es exacto.
        assertThat(service.crearPedido(1, 3, BEARER).total()).isEqualByComparingTo("99999.99");
    }

    @Test
    void elJwtDelUsuarioSePropagaAProductoService() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class)))
                .thenReturn(ResponseEntity.ok(producto(new BigDecimal("1000.00"))));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(3);
            return p;
        });

        service.crearPedido(5, 2, BEARER);

        verify(restTemplate).exchange(eq(url + "5"), eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.argThat(argThatHttpEntityConHeader()), eq(ProductoDTO.class));
    }

    private static org.mockito.ArgumentMatcher<HttpEntity<?>> argThatHttpEntityConHeader() {
        return entity -> BEARER.equals(entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void productoInexistenteTraduce404DeNegocio() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatus.NOT_FOUND, "no encontrado", HttpHeaders.EMPTY, null, null));

        assertThatThrownBy(() -> service.crearPedido(99, 1, BEARER))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void productoServiceCaidoTraduce502DeNegocio() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class)))
                .thenThrow(new ResourceAccessException("conexion rehusada", new ConnectException()));

        assertThatThrownBy(() -> service.crearPedido(1, 1, BEARER))
                .isInstanceOf(ServicioProductoNoDisponibleException.class);
    }

    @Test
    void productoServiceQueRechazaElTokenTraduce502DeNegocio() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(
                        HttpStatus.UNAUTHORIZED, "sin autorizacion", HttpHeaders.EMPTY, null, null));

        assertThatThrownBy(() -> service.crearPedido(1, 1, BEARER))
                .isInstanceOf(ServicioProductoNoDisponibleException.class)
                .hasMessageContaining("401");
    }

    @Test
    void laUrlSeTomaDeLaConfiguracionNoQuemada() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class)))
                .thenReturn(ResponseEntity.ok(producto(new BigDecimal("1000.00"))));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(3);
            return p;
        });

        service.crearPedido(5, 2, BEARER);

        verify(restTemplate).exchange(eq(url + "5"), eq(HttpMethod.GET), any(HttpEntity.class), eq(ProductoDTO.class));
    }
}
