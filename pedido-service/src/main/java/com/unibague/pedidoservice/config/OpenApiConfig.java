package com.unibague.pedidoservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI pedidoOpenApi() {
        String scheme = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("Tienda Deportiva - Pedido Service")
                .version("2.0.0")
                .description("Pedidos de la tienda deportiva, protegidos con JWT (Taller 02 v2.0)."))
            .addSecurityItem(new SecurityRequirement().addList(scheme))
            .components(new Components().addSecuritySchemes(scheme,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
