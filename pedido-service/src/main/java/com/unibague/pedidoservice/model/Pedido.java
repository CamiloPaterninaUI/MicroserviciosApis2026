package com.unibague.pedidoservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int productoId;
    private int cantidad;
    private double total;

    public Pedido() {}
    public Pedido(int id, int productoId, int cantidad, double total) {
        this.id = id; this.productoId = productoId; this.cantidad = cantidad; this.total = total;
    }

    public Pedido(int productoId, int cantidad, double total) {
        this.productoId = productoId; this.cantidad = cantidad; this.total = total;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}