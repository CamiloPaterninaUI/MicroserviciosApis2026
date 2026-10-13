# FINAL_TEST_REPORT.md — Tienda Deportiva v2.0 — Informe Final de Validación

**Proyecto:** evolución de `CamiloPaterninaUI/MicroserviciosApis2026` (Grupo 06) al Taller 02 v2.0.
**Rama:** `taller02-evolucion` (rama original `main` intacta; posibilidad de volver a la versión original garantizada con `git checkout main`).
**Fecha de la validación integral:** 13 de octubre de 2026.

---

## 1. Resumen ejecutivo

La tienda deportiva evolucionó de 2 microservicios (Boot 3.3.4, Java 17, MySQL compartida, sin seguridad, sin pruebas, sin Docker) a **5 microservicios** que cumplen el Taller 02 v2.0 (Java 21, Boot 4.0.8, Spring Cloud 2025.1.3), **conservando el dominio original**: el catálogo, los pedidos, el servicio SOAP con su WSDL y los datos semilla. La validación se ejecutó de verdad: compilación de los 5 módulos, 69 pruebas automatizadas, 9 contenedores con 4 PostgreSQL separadas, 39 pruebas funcionales HTTP por el Gateway, la colección Postman completa con newman (21/21) y persistencia tras reinicio. Durante el proceso se **detectó y corrigió una regresión real de integración** (proteger el catálogo rompía la creación de pedidos) y se re-validó todo tras la corrección.

| Suite | Resultado |
|---|---|
| Maven `clean verify` (5 módulos) | **BUILD SUCCESS — 69/69** |
| Matriz funcional HTTP (script) | **39/39** |
| Postman con newman | **21 solicitudes / 32 aserciones, 0 fallos** |
| Docker Compose | **9/9 servicios, 4 BDs healthy** |
| Persistencia tras reinicio | **Datos, token y SOAP intactos** |

## 2. Estado de los cinco microservicios

| Servicio | Puerto | Estado | Pruebas propias |
|---|---|---|---|
| api-gateway | 8080 | Up; 5 rutas (auth, users, productos, pedidos, **/ws SOAP**); X-Correlation-Id generado/reenviado | 4 |
| auth-service | 8081 | Up; registro/login, BCrypt, JWT RS256, usuarios demo | 17 |
| user-service | 8082 | Up; CRUD perfiles, propietario/ADMIN | 23 |
| producto-service | 8083 | Up; REST protegido + **SOAP /ws operativo**, semillas, NUMERIC(12,2) | 14 (2 de SOAP) |
| pedido-service | 8084 | Up; cálculo BigDecimal, JWT propagado a producto-service, 404/502/400 de negocio | 13 |
| 4 × PostgreSQL 17 | 5433-5436 | `(healthy)`, Flyway V1 en cada una, separación verificada (0 tablas cruzadas) | — |

## 3. Compilación Maven

`mvn -B clean verify` → **BUILD SUCCESS**. Reactor: padre + 5 módulos, todos SUCCESS. Detalle de versiones verificadas contra Maven Central antes de migrar: Boot 4.0.8, `spring-boot-starter-webmvc`, `spring-boot-starter-web-services` (el SOAP sobrevive en Boot 4), Cloud 2025.1.3, springdoc 3.0.1, jaxb-maven-plugin 4.0.x.

## 4. Pruebas unitarias y de integración (69)

- **api-gateway (4):** contexto con las 5 rutas; generación/reenvío/devolución del correlation id.
- **auth-service (17):** API 201/200/400/401/409 con seguridad real; firma RS256 y claims del contrato; BCrypt (nunca texto plano); roles desde BD.
- **user-service (23):** matriz de autorización con tokens RS256 firmados en memoria (401/403/200/201/204/400/404/409), CRUD y propiedad.
- **producto-service (14):** catálogo 401/200/201/403/404/400 con la cadena de filtros real; **SOAP: getProductoRequest → getProductoResponse con el contrato original y fault por inexistente**; lógica de servicio con BigDecimal.
- **pedido-service (13):** total exacto (`85000.00 × 3 = 255000.00`; el caso `33333.33 × 3` que con `double` daba `99999.99000000001` es exacto), **propagación del JWT a producto-service verificada por el header del HttpEntity**, 404 producto inexistente, 502 servicio caído o token rechazado, URL por configuración (no quemada), validación de parámetros.

## 5. Docker

`docker compose config` válido; `up --build -d` construye las 5 imágenes multietapa (Maven dentro del contenedor, usuario no-root) y levanta 9 servicios; healthchecks en las 4 BDs; 4 volúmenes persistentes; `.dockerignore`, `.env.example`, `.gitignore` con llaves/secretos excluidos.

## 6. PostgreSQL y Flyway

- 4 BDs independientes (`auth_db`, `users_db`, `productos_db`, `pedidos_db`), cada una con su migración V1 (`credentials`, `user_profiles`, `producto`, `pedido`) y `flyway_schema_history`; tras reinicio, **ninguna migración se reaplica**.
- Separación verificada por SQL: 0 tablas ajenas en cada BD; ningún servicio comparte conexión.
- JPA con `ddl-auto: validate` arrancó sin errores en los 4 servicios.
- Dinero: `precio NUMERIC(12,2)`, `total NUMERIC(14,2)`; semillas con escala 2 (`120000.00`, `85000.00`, `250000.00`).

## 7. JWT y seguridad

- BCrypt (`$2a$10$`, 60 caracteres) para todas las credenciales; contraseñas jamás en respuestas ni logs.
- JWT RS256 con `kid=taller02-key`; claims del contrato (`sub` UUID, `username`, `email`, `roles`, `iss=taller02-auth`); firma/expiración/issuer validados en cada servicio con la **llave pública**.
- Llave privada **solo** en el jar de auth-service; user/producto/pedido contienen únicamente `public.pem` (verificado dentro de los contenedores); Git sin `.pem` ni `.env`.
- Autorización: catálogo y pedidos → autenticado; crear productos → solo ADMIN; perfiles → propietario/ADMIN; 401 vs 403 correctos en toda la matriz.
- **JWT propagado de servicio a servicio** (pedido → producto) con la identidad del usuario: la regresión inicial (401 silencioso → 500) fue corregida y re-verificada.
- SOAP `/ws/**` público por decisión documentada (interoperabilidad clásica); probado operativo, con fault y tras reinicio.

## 8. CRUD funcional (por el Gateway)

Catálogo: listar (200), consultar (200), crear como ADMIN (201), crear como USER (403), sin token (401), inexistente (404), inválido (400). Pedidos: crear con total exacto (200), producto inexistente (404), cantidad 0 (400), listar (200), sin token (401). Perfiles: crear (201/409), `/me` (200, identidad del token), listar como USER (403) / ADMIN (200). Todos los IDs salieron de respuestas reales.

## 9. Swagger y Postman

- Swagger UI y `/v3/api-docs` operativos en 8081-8084 con `bearerAuth` y los paths del dominio.
- **newman: 21/21 solicitudes y 32/32 aserciones**, en orden, incluyendo la carpeta SOAP (WSDL por el Gateway + operación + fault) y todos los casos negativos. Los scripts guardan `studentToken`, `adminToken`, `productoId`, `pedidoId` reales.

## 10. Persistencia

`docker compose down` (**sin** `-v`) → `up -d`: 7 productos, 3 pedidos (totales exactos), credenciales y perfil intactos; Flyway no reaplicó; el **token emitido antes del reinicio siguió válido** (`GET /me` → 200) y el SOAP respondió con Guayos tras el reinicio. `down -v` no se ejecutó en ningún momento.

## 11. CI/CD

Workflow revisado estáticamente: Java 21 (Temurin) con caché, `mvn -B clean verify`, job docker condicionado a push a `main` con `needs: verify`, permisos de mínimo privilegio (`packages: write` solo en ese job), GHCR con dueño normalizado a minúsculas (`camilopaterninaui`) y tags `latest`+SHA para los 5 módulos. **No se ejecutó en remoto porque la misión prohíbe publicar sin autorización**; se activará al autorizar el push/fusión.

## 12. Errores encontrados y soluciones

| # | Error | Causa | Solución | Re-prueba |
|---|---|---|---|---|
| 1 | `POST /api/pedidos` → 500 | producto-service protegido rechazaba (401) la consulta de precio sin JWT | Propagación del `Authorization` del usuario con `RestTemplate.exchange`; rechazo upstream → 502 | 13/13 módulo + 39/39 funcional ✓ |
| 2 | `';' expected` en `ProductoEndpoint` | `import ... as ...` (sintaxis no válida en Java) | Patrón original: import del tipo WSDL + `var` para la entidad | Compila ✓ |
| 3 | `@WebMvcTest` pedía `ProductoRepository` | CommandLineRunner dentro de la clase de aplicación | Reubicado en `config.DatosInicialesConfig` (semillas intactas) | 14/14 ✓ |
| 4 | `package ...support does not exist` | Clases de prueba en paquete equivocado | Migradas a `com.unibague.{productoservice,pedidoservice}.support` | Compila ✓ |
| 5 | Riesgo de configuración pisada | `application.properties` tiene precedencia sobre `.yml` | Migración a YAML y eliminación de los `.properties` (quedan en `main`) | Arranque ✓ |
| 6 | Aserciones de precio "fallaban" | El script parseaba el JSON con JS (pierde escala decimal) | Verificación sobre texto crudo (`"precio":120000.00`) | 39/39 ✓ |

## 13. Requisitos pendientes

Ninguno técnico. Dos acciones sujetas a autorización explícita del equipo:
1. **Push de la rama `taller02-evolucion`** (el remoto no ha sido modificado; `main` intacto).
2. **Limpieza opcional de datos demo** (`docker compose down -v`).

## 14. Conclusión

El proyecto cumple los requisitos obligatorios del Taller 02 v2.0 **sin sacrificar el trabajo original**: el catálogo, los pedidos y el SOAP funcionan igual que antes (ahora con puertos 8083/8084 y todo también por el Gateway 8080), con dinero exacto, errores de negocio correctos, seguridad JWT RS256 con roles, bases separadas con Flyway, 69 pruebas automatizadas, colección Postman reproducible, Docker Compose completo y CI/CD listo. La única regresión detectada durante la evolución (pedidos vs. catálogo protegido) fue corregida y re-validada de extremo a extremo. **Validación local COMPLETADA; la ejecución remota del workflow queda sujeta al push autorizado.**
