package com.unibague.productoservice.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.unibague.productoservice.model.Producto;
import com.unibague.productoservice.repository.ProductoRepository;

/**
 * Conserva los datos iniciales originales de la tienda deportiva
 * (camiseta, balon y guayos). Vive en una @Configuration separada para
 * que la clase de aplicacion quede limpia y los slices de test no
 * arrastren dependencias de persistencia.
 */
@Configuration
public class DatosInicialesConfig {

    @Bean
    CommandLineRunner cargarDatosIniciales(ProductoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new Producto("Camiseta Selección Colombia", BigDecimal.valueOf(120000), 50));
                repository.save(new Producto("Balón de fútbol", BigDecimal.valueOf(85000), 30));
                repository.save(new Producto("Guayos Predator", BigDecimal.valueOf(250000), 15));
            }
        };
    }
}
