# MicroserviciosApis2026

Proyecto de la asignatura Web, APIs y Microservicios — Universidad de Ibagué.

Aplicación web compuesta por dos microservicios independientes construidos con Spring Boot, que en conjunto implementan un servicio web SOAP, una API y una API REST, comunicándose entre sí mediante peticiones REST.

## Arquitectura

- **producto-service** (puerto 8081): expone un servicio web SOAP y una API REST para la gestión de productos.
- **pedido-service** (puerto 8082): expone una API REST para la gestión de pedidos y consume la API REST de producto-service para calcular el valor total de cada pedido.

## Tecnologías

- Java 17
- Spring Boot 3.3.4
- Spring Web / Spring Web Services
- Maven
- RestTemplate

## Endpoints

### producto-service

| Tipo | Endpoint | Descripción |
|---|---|---|
| SOAP | `POST /ws` | Consulta de producto (`getProductoRequest` / `getProductoResponse`) |
| WSDL | `GET /ws/productos.wsdl` | Definición del servicio web |
| REST | `GET /api/productos` | Lista todos los productos |
| REST | `GET /api/productos/{id}` | Consulta un producto por id |
| REST | `POST /api/productos` | Crea un producto |

### pedido-service

| Tipo | Endpoint | Descripción |
|---|---|---|
| REST | `GET /api/pedidos` | Lista todos los pedidos |
| REST | `POST /api/pedidos?productoId={id}&cantidad={n}` | Crea un pedido consultando el precio en producto-service |

## Cómo correr el proyecto

```bash
cd producto-service
mvn spring-boot:run
```

```bash
cd pedido-service
mvn spring-boot:run
```

## Equipo

- Camilo Paternina
- Julian Fonseca
- Santiago Lozano 
- Laura Zalazar