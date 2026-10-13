package com.unibague.productoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio del catalogo deportivo: REST + SOAP (WSDL en /ws).
 * La carga de datos iniciales vive en config.DatosInicialesConfig.
 */
@SpringBootApplication
public class ProductoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductoServiceApplication.class, args);
    }
}
