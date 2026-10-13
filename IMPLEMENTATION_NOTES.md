# IMPLEMENTATION_NOTES.md — Evolución de la Tienda Deportiva al Taller 02 v2.0

**Repositorio:** `CamiloPaterninaUI/MicroserviciosApis2026` (Grupo 06, Universidad de Ibagué).
**Rama de trabajo:** `taller02-evolucion` (la rama `main` conserva la versión original intacta).
**Fuente de requisitos:** documento Taller 02 v2.0 (guía integral, anexos A.1–A.53) — versión transcrita y validada previamente por el equipo.
**Regla de oro aplicada:** conservar el dominio original (productos, pedidos y SOAP), adaptando los **patrones** del proyecto de referencia, nunca copiándolos a ciegas.

---

## 1. Matriz de auditoría: requisito → evidencia actual → brecha → cambio aplicado

| # | Requisito Taller 02 | Evidencia en el repo original | Brecha | Cambio aplicado |
|---|---|---|---|---|
| 1 | Java 21 LTS | `<java.version>17</java.version>` | ❌ | Módulos actualizados a 21 |
| 2 | Spring Boot 4.0.8 | Boot 3.3.4 con `spring-boot-starter-web` | ❌ | Boot 4.0.8 con `spring-boot-starter-webmvc` (nombre real de Boot 4, verificado en Maven Central) |
| 3 | Maven multimódulo con POM raíz | Dos POMs huérfanos sin padre común | ❌ | POM raíz `microservicios-tienda` + 5 módulos |
| 4 | Spring Cloud 2025.1.3 / Gateway 5.x | No existía | ❌ | Nuevo `api-gateway` (WebFlux) con `spring-cloud-starter-gateway-server-webflux` |
| 5 | Auth Service (8081) | No existía | ❌ | `auth-service` nuevo, patrón del taller adaptado al paquete `com.unibague.auth` |
| 6 | User Service (8082) | No existía | ❌ | `user-service` nuevo (`com.unibague.users`) |
| 7 | PostgreSQL independiente por servicio | **Una sola MySQL compartida** `microservicios_db` (root sin contraseña) | ❌ | 4 PostgreSQL 17-alpine: `auth_db`, `users_db`, `productos_db`, `pedidos_db` (puertos 5433-5436) |
| 8 | Flyway V1 + `ddl-auto: validate` | `ddl-auto=update`, sin migraciones | ❌ | 4 migraciones versionadas + `validate` |
| 9 | JWT RS256 (privada emisor / pública validador) | No existía | ❌ | auth firma con la privada; user/producto/pedido validan con la pública (script de generación) |
| 10 | BCrypt + roles USER/ADMIN | No existía | ❌ | Implementado con usuarios demo `admin`/`student` |
| 11 | DTOs, validación, manejo global de errores | Entidades JPA expuestas; `RuntimeException` → 500; sin `@Valid` | ❌ | DTOs (records), `@Valid`, `GlobalExceptionHandler` con 400/404/409/502 reales |
| 12 | Swagger/OpenAPI | No existía | ❌ | springdoc 3.0.1 en los 4 servicios de negocio con `bearerAuth` |
| 13 | Docker Compose con healthchecks y volúmenes | No existía | ❌ | Compose de 9 servicios (4 BDs + 5 apps), healthchecks, 4 volúmenes |
| 14 | Dockerfiles multietapa | No existía | ❌ | 5 Dockerfiles (Maven → JRE, usuario no-root) |
| 15 | Pruebas automatizadas | **Cero pruebas** | ❌ | 69 pruebas: unitarias, rebanada web con seguridad real, SOAP y de servicio |
| 16 | Colección Postman + CI/CD | No existía | ❌ | `postman/` (dominio tienda, incluye SOAP) + workflow con verify y GHCR |
| 17 | Conservar productos, pedidos y SOAP | REST + SOAP `/ws` + WSDL + seed de 3 productos | ✅ | Se conserva TODO; solo cambia el puerto directo (ver §2) |
| 18 | Corregir `double` monetario | `double precio`, `double total`, `xs:double` | ❌ | `BigDecimal` en entidad/DTO/JSON + `NUMERIC(12,2)`/`NUMERIC(14,2)` + `xs:decimal` |
| 19 | Corregir localhost fijo | `PRODUCTO_SERVICE_URL = "http://localhost:8081/api/productos/"` quemado | ❌ | Propiedad `producto.service.url` inyectada por entorno |
| 20 | Errores HTTP genéricos | `RuntimeException` en todo | ❌ | 404 producto inexistente, 502 producto-service caído, 400 validación, 409 duplicados |
| 21 | Proteger endpoints existentes sin romperlos | Todo abierto | ❌ | JWT en REST: catálogo y pedidos requieren token; crear producto exige ADMIN; **SOAP público** (§5) |

## 2. Conflicto de puertos y decisión documentada

El Taller 02 fija `auth-service:8081` y `user-service:8082`, que en la tienda ocupaban producto y pedido. Decisión: **auth 8081 y users 8082 según el contrato del taller; producto → 8083 y pedido → 8084**. Ninguna operación se pierde: las rutas y contratos REST/SOAP son idénticos y, además, todo queda accesible por el Gateway 8080 (`/api/productos/**`, `/api/pedidos/**`, `/ws/**`). El cambio está reflejado en el README y en la configuración por variables (`PRODUCTO_SERVICE_URL`).

## 3. Migración MySQL → PostgreSQL (Database per Service)

La base compartida `microservicios_db` violaba el patrón Database-per-Service del taller y la guía exige PostgreSQL 17. Cada servicio posee ahora su BD aislada (sin usuarios ni conexiones cruzadas), con esquemas creados por Flyway y verificados por Hibernate (`ddl-auto: validate`). La lógica JPA se conserva (mismos repositorios); solo cambian el driver y las migraciones.

## 4. Correcciones de código en el dominio (sin romper contratos)

1. **Dinero con `BigDecimal`:** `Producto.precio`, `Pedido.total` y `ProductoDTO.precio` dejaron de ser `double`. El cálculo del pedido usa `multiply` + `setScale(2, HALF_UP)`. El XSD SOAP cambió `xs:double` → `xs:decimal` (el número en el wire es idéntico; los clientes SOAP existentes siguen interoperando — verificado en las pruebas).
2. **DTOs y validación:** el catálogo ya no expone la entidad JPA (`ProductoRequest`/`ProductoResponse` con `@NotBlank`/`@DecimalMin`/`@Min`); los pedidos responden con `PedidoResponse`. El contrato original de pedidos (`POST /api/pedidos?productoId=&cantidad=`) se conservó exactamente, sumando validación (`@Validated` + `@Min`).
3. **Errores de negocio:** `ResourceNotFoundException` (→404), `ProductoNoEncontradoException` (→404 cuando producto-service responde 404), `ServicioProductoNoDisponibleException` (→502 cuando producto-service está caído), validación (→400 con detalle por campo). El SOAP conserva su semántica de fault.
4. **URL configurable:** `producto.service.url` (default `http://localhost:8083/api/productos/`, inyectada por Compose como `http://producto-service:8083/api/productos/`). `RestTemplate` con timeouts de 3s/5s (antes un producto-service caído bloqueaba indefinidamente).
5. **Seed original conservado:** camiseta/balón/guayos se cargan igual que antes, reubicados en `config.DatosInicialesConfig` (patrón de `DemoDataConfig` del taller) para que la clase de aplicación quede limpia y los slices de prueba no arrastren persistencia.
6. **Capa de servicio en producto:** el controlador y el endpoint SOAP ahora comparten `ProductoService` (misma lógica y mismos errores en REST y SOAP).

## 5. Seguridad aplicada y decisión SOAP

- Todos los servicios de negocio son **OAuth2 Resource Servers**: validan firma RS256, expiración e issuer (`taller02-auth`) con la **llave pública** (nunca la privada — verificado dentro de los jars y en Git).
- Autorización: catálogo y pedidos → autenticado; `POST /api/productos` → `ROLE_ADMIN`; perfiles → propietario/ADMIN según contrato del taller.
- **El SOAP `/ws/**` se mantiene público**, decisión consciente para no romper a los clientes SOAP clásicos del proyecto original (un header `Authorization` no forma parte del estilo SOAP de la asignatura). Está documentado como riesgo aceptado del entorno académico y es validado explícitamente en las pruebas de compatibilidad.

## 6. Verificación de versiones (compatibilidad real)

Verificado contra Maven Central antes de codificar (2026-10-13): `spring-boot-starter-parent` 4.0.8 ✓, `spring-boot-starter-webmvc` (nuevo nombre de Boot 4) ✓, `spring-boot-starter-web-services` **existe en 4.0.8** (el SOAP sobrevive el upgrade) ✓, `spring-cloud-dependencies` 2025.1.3 ✓, `spring-cloud-starter-gateway-server-webflux` 5.x ✓, `springdoc` 3.0.1 ✓, `flyway-database-postgresql` ✓, `jaxb-maven-plugin` 4.0.x ✓. La compilación real confirmó la compatibilidad (BUILD SUCCESS, 69 pruebas).

## 7. Correcciones técnicas durante la implementación (hallazgos reales)

1. `import ... as ...` usado por error en `ProductoEndpoint` (sintaxis inexistente en Java) → corregido al patrón del código original (import del tipo WSDL + `var` para la entidad).
2. Los archivos `application.properties` originales **debían eliminarse**: Spring les da precedencia sobre `application.yml` y habrían sobrescrito el datasource/ Flyway. Se migraron a `application.yml` y los `.properties` se borraron (quedan en el historial de `main`).
3. El CommandLineRunner en la clase de aplicación rompía los slices `@WebMvcTest` (pedía el repositorio) → reubicado en `DatosInicialesConfig`.
4. Pruebas de soporte RSA/JWT inicialmente creadas en paquete incorrecto (`com.unibague.producto.support` en vez de `com.unibague.productoservice.support`) → corregido.
5. Soporte de tests copiado del taller: `TestJwtFactory.validToken` usa 5 parámetros; los tests nuevos se ajustaron a ese contrato.

## 8. Componentes añadidos sin eliminar nada

api-gateway (rutas + `X-Correlation-Id`), auth-service, user-service, migraciones Flyway ×4, DTOs, excepciones y handlers ×2, SecurityConfig ×3 (user/producto/pedido), OpenApiConfig ×4, pruebas (69), Dockerfiles ×5, Compose, `.env.example`, `.dockerignore`, `.gitattributes`, `scripts/generate-rsa-keys.sh`, Postman ×2, workflow CI/CD, README/IMPLEMENTATION_NOTES/VALIDATION_REPORT/FINAL_TEST_REPORT. La única eliminación fue la de los `application.properties` (§7.2), reemplazados funcionalmente por los `application.yml`.

## 9. Advertencias de seguridad del entorno académico

Las llaves `.pem` generadas por el script y el `.env` no se versionan (`.gitignore`). Los usuarios demo existen solo para el taller. `package-lock.json` (reliquia del repo original) se conserva intacta por decisión de no eliminar archivos del equipo; no afecta al build de Maven.
