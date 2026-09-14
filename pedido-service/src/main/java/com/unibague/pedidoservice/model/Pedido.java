package com.unibague.pedidoservice.model;

public class Pedido {
    private int id;
    private int productoId;
    private int cantidad;
    private double total;

    public Pedido() {}
    public Pedido(int id, int productoId, int cantidad, double total) {
        this.id = id; this.productoId = productoId; this.cantidad = cantidad; this.total = total;
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