package com.unibague.productoservice;

import com.unibague.productoservice.model.Producto;
import com.unibague.productoservice.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProductoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductoServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner cargarDatosIniciales(ProductoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Producto("Camiseta Selección Colombia", 120000, 50));
                repository.save(new Producto("Balón de fútbol", 85000, 30));
                repository.save(new Producto("Guayos Predator", 250000, 15));
            }
        };
    }
}