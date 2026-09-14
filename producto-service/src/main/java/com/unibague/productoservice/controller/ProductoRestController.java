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
    public List<Producto> listar() { return repository.listarTodos(); }

    @GetMapping("/{id}")
    public Producto buscarPorId(@PathVariable int id) { return repository.buscarPorId(id); }

    @PostMapping
    public Producto crear(@RequestBody Producto producto) { return repository.guardar(producto); }
}