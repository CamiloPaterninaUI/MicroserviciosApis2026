package com.unibague.pedidoservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * Crea pedidos consultando el precio en producto-service por HTTP.
 * Evolucion Taller 02 v2.0:
 *  - la URL ya no esta quemada (propiedad producto.service.url),
 *  - el total se calcula con BigDecimal (antes double),
 *  - el JWT del usuario autenticado se PROPAGA a producto-service:
 *    desde que el catalogo esta protegido, la consulta del precio se
 *    realiza con la identidad de quien crea el pedido,
 *  - los fallos se traducen a errores de negocio (404 / 502).
 */
@Service
public class PedidoService {

    private final RestTemplate restTemplate;
    private final PedidoRepository pedidoRepository;
    private final String productoServiceUrl;

    public PedidoService(RestTemplate restTemplate,
                         PedidoRepository pedidoRepository,
                         @Value("${producto.service.url}") String productoServiceUrl) {
        this.restTemplate = restTemplate;
        this.pedidoRepository = pedidoRepository;
        this.productoServiceUrl = productoServiceUrl;
    }

    @Transactional
    public PedidoResponse crearPedido(int productoId, int cantidad, String authorizationHeader) {
        ProductoDTO producto = consultarProducto(productoId, authorizationHeader);

        BigDecimal total = producto.getPrecio()
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);

        Pedido pedido = new Pedido(productoId, cantidad, total);
        return toResponse(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidos() {
        return pedidoRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ProductoDTO consultarProducto(int productoId, String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        try {
            ResponseEntity<ProductoDTO> respuesta = restTemplate.exchange(
                    productoServiceUrl + productoId, HttpMethod.GET, new HttpEntity<>(headers), ProductoDTO.class);
            ProductoDTO producto = respuesta.getBody();
            if (producto == null) {
                throw new ServicioProductoNoDisponibleException(
                        "producto-service no devolvió datos para el producto " + productoId);
            }
            return producto;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ProductoNoEncontradoException("Producto no encontrado en producto-service con id " + productoId);
        } catch (ResourceAccessException ex) {
            throw new ServicioProductoNoDisponibleException(
                    "No fue posible contactar al producto-service: " + ex.getMessage());
        } catch (HttpClientErrorException ex) {
            throw new ServicioProductoNoDisponibleException(
                    "producto-service rechazó la consulta (HTTP " + ex.getStatusCode().value() + ")");
        }
    }

    private PedidoResponse toResponse(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getProductoId(), pedido.getCantidad(), pedido.getTotal());
    }
}
