package com.unibague.productoservice.controller;

import com.unibague.productoservice.model.Producto;
import com.unibague.productoservice.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

    @Autowired
    private ProductoRepository repository;

    @GetMapping
    public List<Producto> listar() { return repository.findAll(); }

    @GetMapping("/{id}")
    public Producto buscarPorId(@PathVariable int id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con id " + id));
    }

    @PostMapping
    public Producto crear(@RequestBody Producto producto) { return repository.save(producto); }
}