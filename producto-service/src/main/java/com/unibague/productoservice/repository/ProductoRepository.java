package com.unibague.productoservice.repository;

import com.unibague.productoservice.model.Producto;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class ProductoRepository {

    private final Map<Integer, Producto> productos = new HashMap<>();

    public ProductoRepository() {
        productos.put(1, new Producto(1, "Camiseta Selección Colombia", 120000, 50));
        productos.put(2, new Producto(2, "Balón de fútbol", 85000, 30));
        productos.put(3, new Producto(3, "Guayos Predator", 250000, 15));
    }

    public Producto buscarPorId(int id) { return productos.get(id); }
    public List<Producto> listarTodos() { return new ArrayList<>(productos.values()); }
    public Producto guardar(Producto producto) { productos.put(producto.getId(), producto); return producto; }
}