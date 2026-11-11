# Arquitectura - Proyecto 03 (Tienda Deportiva v3.0)

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**

## 1. Visión general

La Tienda Deportiva v3.0 evoluciona la versión v2.0 (Taller 02) para incorporar **Kubernetes** y **Keycloak** como proveedor de identidad único (OAuth 2.0 / OpenID Connect), conservando todas las funcionalidades actuales (autenticación, usuarios, productos, pedidos y SOAP) y añadiendo el servicio de **pagos** que aparece en la arquitectura de Proyecto 03.

## 2. Diagrama de componentes (texto)

```
                          +---------------------+
                          |   Cliente / Gateway |
                          |   (puerto 8080)     |
                          +---------------------+
                                   |
                                   | DNS interno de Kubernetes
                                   v
                          +---------------------+
                          |   api-gateway       |  ClusterIP :8080
                          |  (Spring Cloud GW)  |
                          +---------------------+
                /         |         \         \
               /          |          \         \
              v           v           v         v
    +-------------+  +-------------+  +-------------+  +-------------+
    | auth-service|  | user-service|  | producto-svc|  | pedido-svc  |
    |   :8081     |  |    :8082    |  |     :8083   |  |     :8084   |
    +-------------+  +-------------+  +-------------+  +-------------+
           |                   |             |             |
           | (delega)          |             |             |
           v                   v             v             v
    +---------------------+  +-------------+  +-------------+  +-------------+
    |   Keycloak :8080    |  |  users_db   |  | productos_db|  |  pedidos_db |
    | (OAuth2 / OIDC,     |  | PostgreSQL  |  | PostgreSQL  |  | PostgreSQL  |
    |  único emisor JWT)  |  |             |  |             |  |             |
    +---------------------+  +-------------+  +-------------+  +-------------+
           |
           v
    +---------------------+
    |   keycloak_db       |
    |   PostgreSQL        |
    +---------------------+

    +-------------+
    | pago-service|  (NUEVO - Proyecto 03)
    |     :8085   |
    +-------------+
           |
           v
    +-------------+
    |  pagos_db   |
    | PostgreSQL  |
    +-------------+
```

### Comunicación interna
Toda la comunicación entre Pods usa **DNS interno de Kubernetes** (nunca `localhost`):
- `http://keycloak:8080/...` (auth-service → Keycloak; apps → validación de tokens vía JWKS)
- `http://user-service:8082`, `http://producto-service:8083`, `http://pedido-service:8084`, `http://pago-service:8085` (gateway → servicios)
- `http://producto-service:8083/api/productos/` (pedido-service consulta precios)
- `jdbc:postgresql://<servicio-db>:5432/<bd>` (cada app → su PostgreSQL)

## 3. Patrones arquitectónicos conservados

| Patrón | Descripción |
|---|---|
| Database per Service | Cada servicio de negocio tiene su propia PostgreSQL; no hay esquemas ni conexiones compartidas entre servicios |
| DTO | Las APIs expuen records (DTOs), nunca entidades JPA |
| Flyway | Migraciones versionadas (`V1__*.sql`) con `ddl-auto: validate` |
| Validación | `@Valid` / `@Validated` en los DTOs de entrada |
| Manejo global de errores | `GlobalExceptionHandler` por servicio con formato `ApiError` consistente |
| Health / Actuator | `/actuator/health` con readiness y liveness probes en cada servicio |
| Swagger / OpenAPI | Documentación en `/swagger-ui.html` con esquema `bearerAuth` |

## 4. Decisiones de diseño (D)

### D-01: Versión de Spring Boot
El título del documento de requisitos menciona "Spring Boot 3.0", pero el proyecto base ya utiliza **Spring Boot 4.0.8** (Java 21). Se verificó la compatibilidad real contra Maven Central antes de implementar (spring-boot-starter-parent 4.0.8, spring-boot-starter-webmvc, spring-boot-starter-web-services para SOAP, spring-cloud 2025.1.3, springdoc 3.0.1, flyway-database-postgresql, jaxb 4.0.x). La compilación y las 70+ pruebas pasan con Boot 4.0.8. Cambiar a Boot 3.0 solo por el título del documento, sin analizar el impacto, sería innecesario y arriesgado.

### D-02: Un único emisor JWT (Keycloak)
La regla de arquitectura #5 exige evitar dos emisores JWT contradictorios. El Taller 02 usaba `auth-service` como emisor con llaves RSA propias (`taller02-auth`). En Proyecto 03, **Keycloak es el único emisor**. auth-service deja de emitir JWTs y delega registro/login en Keycloak. Los servicios de negocio validan los tokens de Keycloak mediante descubrimiento OIDC (JWKS). Esto elimina la necesidad de distribuir llaves RSA públicas/privadas entre servicios.

### D-03: Contratos de pago (decisiones propias)
El PDF no especifica los contratos del servicio de pagos. Se definen propios y se documentan como tales (no son requisitos expresos del PDF):

| Endpoint | Método | Descripción | Roles |
|---|---|---|---|
| `/api/pagos` | POST | Crea un pago para un pedido (estado PENDIENTE) | USER, ADMIN |
| `/api/pagos/{id}` | GET | Obtiene un pago por ID | USER, ADMIN |
| `/api/pagos/pedido/{pedidoId}` | GET | Obtiene el pago de un pedido | USER, ADMIN |
| `/api/pagos/{id}/estado` | POST | Actualiza el estado de un pago (webhook interno) | ADMIN |

Entidades: `Pago(id, pedidoId, monto NUMERIC(12,2), metodoPago, estado PENDIENTE/APROBADO/RECHAZADO, creadoEn)`. El dinero se maneja con `BigDecimal` (no `double`). No hay integración con pasarela de pago externa real; el webhook usa un secreto HMAC generado con OpenSSL (ver docs/SECURITY.md).

### D-05: SOAP mantenido público
El endpoint SOAP `/ws/**` se mantiene público para conservar la interoperabilidad con clientes clásicos del proyecto original. Decisión documentada en docs/SECURITY.md (riesgo aceptado del entorno académico).

### D-06: auth-service sin PostgreSQL propia
Como la identidad vive en Keycloak, auth-service no tiene base de datos propia; solo delega registro y login en Keycloak mediante su API de administración (client_credentials/password grant con el admin de Keycloak).

### D-07: Puerto del servicio de pagos
El pago-service ocupa el puerto **8085** para evitar conflictos con pedido-service (8084), cumpliendo la regla de arquitectura #8.

### D-08: Realm configurado mediante Job de Kubernetes
La importación del realm vía `start-dev` de Keycloak resultó inconsistente (se cuelga en la importación). Se usa un `Job` (`keycloak-realm-config`) que ejecuta `kcadm.sh` contra la API de administración de Keycloak para crear el realm `tienda-deportiva`, los roles `USER`/`ADMIN` y el cliente público `tienda-auth` (concesión de contraseña OIDC).

### D-09: Pruebas con emisor de prueba
Las pruebas unitarias de los servicios de negocio usan un emisor de prueba (`test-auth`) con llaves RSA generadas en memoria, para no depender de un clúster Keycloak real en el alcance de prueba unitaria. La integración real contra Keycloak se valida en Kubernetes (ver FINAL_TEST_REPORT_PROYECTO03.md).

## 5. Diferencias con la arquitectura del ejemplo del profesor

| Aspecto | Ejemplo del profesor (PDF) | Tienda Deportiva v3.0 | Motivo |
|---|---|---|---|
| Spring Boot | 3.0 (título del documento) | 4.0.8 | Decisión D-01 (compatibilidad verificada) |
| Emisor JWT | Posiblemente dos emisores contradictorios | Un único emisor (Keycloak) | Regla de arquitectura #5 |
| Servicio de pagos | Aparece en la arquitectura del ejemplo | Implementado con contratos propios (D-03) | Requisito R-05 |
| Puerto del pedido-service | Específico del ejemplo | 8084 (evita conflictos con auth 8081/users 8082) | Requisito R-06, regla #8 |
| Base de datos | Posiblemente una sola compartida | PostgreSQL independiente por servicio | Database per Service (T-05, R-11) |
| Gestión de identidad | No especificada en detalle | Keycloak como único emisor JWT | Requisito R-04, regla #5 |
