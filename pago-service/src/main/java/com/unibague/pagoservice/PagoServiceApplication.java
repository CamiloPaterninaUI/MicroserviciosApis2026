package com.unibague.pagoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pago-service: servicio de pagos de la Tienda Deportiva (Proyecto 03).
 * Recurso OAuth2 protegido por Keycloak (emisor unico JWT).
 */
@SpringBootApplication
public class PagoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagoServiceApplication.class, args);
    }
}
