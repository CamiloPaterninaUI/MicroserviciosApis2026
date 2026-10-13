package com.unibague.pedidoservice.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pedido de la tienda. El total monetario usa BigDecimal (antes double).
 * Solo se persisten el id de producto, la cantidad y el total calculado.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "producto_id", nullable = false)
    private Integer productoId;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    public Pedido() {}

    public Pedido(Integer productoId, Integer cantidad, BigDecimal total) {
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.total = total;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getProductoId() { return productoId; }
    public void setProductoId(Integer productoId) { this.productoId = productoId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
