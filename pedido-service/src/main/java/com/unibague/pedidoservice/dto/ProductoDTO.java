package com.unibague.pedidoservice.dto;

import java.math.BigDecimal;

/**
 * Vista del producto que expone producto-service por REST y que este
 * servicio consume. El precio viaja como BigDecimal (JSON numerico),
 * reemplazando el double original.
 */
public class ProductoDTO {

    private Integer id;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
