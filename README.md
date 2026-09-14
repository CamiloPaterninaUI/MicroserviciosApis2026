# MicroserviciosApis2026

Proyecto de la asignatura Web, APIs y Microservicios (Universidad de Ibagué) — Grupo 06.

La idea del proyecto es simular un pequeño sistema de tienda deportiva usando dos microservicios que no comparten base de código entre sí, pero se comunican por HTTP. Uno de los dos incluso expone un servicio SOAP viejo, para practicar los dos estilos (SOAP y REST) dentro del mismo trabajo.

## De qué trata

- **producto-service** (puerto 8081): maneja el catálogo de productos. Tiene una API REST normal y, además, un servicio SOAP con su propio WSDL, para consultar un producto por id al estilo "clásico".
- **pedido-service** (puerto 8082): maneja los pedidos. Cuando alguien crea un pedido, este servicio le pregunta a producto-service (por REST) cuánto vale el producto, y con eso calcula el total del pedido.

Cada microservicio funciona de forma independiente y no se meten en el código del otro. Toda la comunicación entre ellos pasa por HTTP.

## Cómo está armado por dentro

Al principio ambos servicios guardaban todo en memoria (un `HashMap`), así que si reiniciabas la aplicación se perdía todo. Ya lo cambiamos para que persista en una base de datos MySQL real usando Spring Data JPA. Las clases `Producto` y `Pedido` ahora son entidades (`@Entity`) y en vez de manejar listas a mano, usamos interfaces `JpaRepository` que Spring implementa automáticamente.

## Tecnologías

- Java 17
- Spring Boot 3.3.4
- Spring Web + Spring Web Services (para la parte SOAP)
- Spring Data JPA + MySQL
- Maven
- RestTemplate (para que pedido-service llame a producto-service)

## Antes de correrlo

Necesitas tener MySQL corriendo en tu máquina (nosotros usamos XAMPP porque trae MySQL y phpMyAdmin juntos, más fácil). Hay que crear una base de datos vacía llamada `microservicios_db`:

```sql
CREATE DATABASE microservicios_db;
```

No hace falta crear las tablas a mano — Spring/Hibernate las crea solas la primera vez que arranca producto-service (queda configurado con `ddl-auto=update` en el `application.properties`).

Si tu MySQL tiene usuario o contraseña distintos al típico `root` sin contraseña, cámbialo en:

- `producto-service/src/main/resources/application.properties`
- `pedido-service/src/main/resources/application.properties`

## Cómo correrlo

Cada microservicio se levanta por separado, en su propia terminal. Primero producto-service:

```bash
cd producto-service
mvn spring-boot:run
```

Espera a que salga `Started ProductoServiceApplication` antes de seguir. Ahí ya debería haber creado las tablas y metido los 3 productos de ejemplo (camiseta, balón y guayos).

Luego, en otra terminal, pedido-service:

```bash
cd pedido-service
mvn spring-boot:run
```

Este necesita que producto-service ya esté arriba, porque en cuanto le pidas crear un pedido va a intentar consultarle el precio del producto.

## Endpoints

### producto-service (puerto 8081)

| Tipo | Endpoint | Qué hace |
|---|---|---|
| REST | `GET /api/productos` | Lista todos los productos |
| REST | `GET /api/productos/{id}` | Trae un producto por id |
| REST | `POST /api/productos` | Crea un producto nuevo |
| SOAP | `POST /ws` | Consulta un producto (`getProductoRequest` → `getProductoResponse`) |
| WSDL | `GET /ws/productos.wsdl` | El contrato del servicio SOAP |

### pedido-service (puerto 8082)

| Tipo | Endpoint | Qué hace |
|---|---|---|
| REST | `GET /api/pedidos` | Lista todos los pedidos |
| REST | `POST /api/pedidos?productoId={id}&cantidad={n}` | Crea un pedido (calcula el total consultando el precio en producto-service) |

Para probar el POST no sirve el navegador solo, toca usar Postman o algo parecido, ya que necesita parámetros.

## Equipo

- Camilo Paternina
- Julian Fonseca
- Santiago Lozano
- Laura Zalazar
