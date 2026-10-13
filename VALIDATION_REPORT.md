# VALIDATION_REPORT.md — Tienda Deportiva v2.0 — Evidencia de la validación

**Fecha:** 13 de octubre de 2026. **Rama:** `taller02-evolucion` (validación local; `main` conserva el original).
**Entorno:** Windows 11, Temurin 21.0.12.101, Maven 3.9.16, Docker Engine 29.8.2, Compose v5.6.0.
**Regla de honestidad:** solo se registra lo que se ejecutó realmente. Los fallos hallados y sus correcciones están documentados.

---

## 1. Matriz de verificación ejecutada

| # | Prueba | Comando | Esperado | Obtenido | Estado | Evidencia / corrección |
|---|---|---|---|---|---|---|
| 1 | Compilación + pruebas | `mvn -B clean verify` | BUILD SUCCESS | **EXIT 0 — 5/5 módulos, 69 pruebas (4+17+23+14+11), 0 fallos** | APROBADO | Requerido 3 iteraciones de corrección (ver §3) |
| 2 | Config Compose | `docker compose config --quiet` | exit 0 | exit 0 | APROBADO | — |
| 3 | Levantar todo | `docker compose up --build -d` | 9 servicios | 9/9 Up; 4 BDs `(healthy)` | APROBADO | Build de 5 imágenes Maven-multietapa exitosa |
| 4 | Health Gateway | `GET :8080/actuator/health` | 200 UP | 200 `{"status":"UP"}` | APROBADO | — |
| 5 | Health auth/user/producto/pedido | `GET :8081..:8084/actuator/health` | 200 UP ×4 | 200 UP ×4 | APROBADO | — |
| 6 | Flyway ×4 | `docker compose logs` + SQL | V1 aplicada, no reaplicada | "Migrating schema... create producto/pedido" applied; historial=1 en cada BD | APROBADO | — |
| 7 | JPA validate | arranque de 4 servicios | sin error de esquema | Started sin SchemaManagementException | APROBADO | — |
| 8 | Separación de BDs | SQL `information_schema` en las 4 BDs | 0 tablas ajenas | productos_db, pedidos_db, auth_db, users_db: 0 tablas cruzadas | APROBADO | Database-per-Service real |
| 9 | Tipo monetario en BD | SQL columnas | NUMERIC(12,2)/NUMERIC(14,2) | `numeric 12,2` confirmado | APROBADO | — |
| 10 | Semillas originales | SQL `producto` | 3 productos (camiseta/balón/guayos) | 120000.00 / 85000.00 / 250000.00 presentes | APROBADO | Conservadas tras la evolución |
| 11 | BCrypt | SQL `credentials` | hashes `$2a$10$`, no texto plano | admin/student/test: `$2a$10$` (60) | APROBADO | — |
| 12 | Registro nuevo / duplicado / inválido | HTTP por :8080 | 201 / 409 / 400 | 201, 409, 400 con `validationErrors` por campo | APROBADO | — |
| 13 | Login ADMIN/USER/incorrecto | HTTP por :8080 | 200+JWT / 200+JWT / 401 | según contrato, token RS256 623/627 car. | APROBADO | Tokens nunca impresos completos |
| 14 | Claims JWT | decodificación header/payload | RS256, sub UUID, username, email, roles, iss | RS256/kid=taller02-key; iss=taller02-auth; roles ROLE_USER | APROBADO | — |
| 15 | JWT alterado | payload reescrito | 401 | 401 | APROBADO | — |
| 16 | JWT expirado | firmado localmente exp −1 h | 401 | 401 | APROBADO | — |
| 17 | Perfiles (Taller 02) | POST /api/users, /me, lista | 201/409, 200, 401, 403, 200 | Todos según contrato | APROBADO | 409 en re-corridas = contrato duplicado |
| 18 | Catálogo protegido | GET/POST /api/productos | 401 sin token, 200 con JWT, 403 USER crea, 201 ADMIN crea, 404 inexistente, 400 inválido | Exactamente ese comportamiento | APROBADO | — |
| 19 | Precio exacto en JSON | texto crudo de la respuesta | `"precio":120000.00` | `"precio":120000.00` (escala 2) | APROBADO | — |
| 20 | Pedidos: creación | POST /api/pedidos?productoId=2&cantidad=3 | 200 con `total` 255000.00 exacto | `"total":255000.00` | APROBADO | **Fallaba con 500** — corregido (ver §3.1) |
| 21 | Pedidos: producto inexistente | productoId=9999 | 404 | 404 "Producto no encontrado en producto-service con id 9999" | APROBADO | Corregido junto al 20 |
| 22 | Pedidos: cantidad 0 / sin token | cantidad=0; GET sin JWT | 400 / 401 | 400 / 401 | APROBADO | — |
| 23 | **SOAP WSDL** | GET :8080/ws/productos.wsdl | 200, contrato original | 200 con definitions + namespace `http://unibague.edu.co/productos` + ProductoPort | APROBADO | Enrutado por el Gateway |
| 24 | **SOAP operación** | POST :8080/ws (getProductoRequest id=1) | getProductoResponse con la camiseta | 200 XML con `getProductoResponse` + `Camiseta` | APROBADO | Compatibilidad confirmada |
| 25 | **SOAP fault** | id=9999 | SOAP Fault | Fault presente (HTTP 500 estándar de SOAP) | APROBADO | — |
| 26 | SOAP tras reinicio | id=3 post-reinicio | 200 con Guayos | 200 + Guayos | APROBADO | — |
| 27 | Gateway X-Correlation-Id | header propio / ausente | reenviado / generado | `corr-tienda-123` devuelto; UUID generado | APROBADO | — |
| 28 | Colección Postman | `newman run` (21 solicitudes / 32 aserciones) | 0 fallos | **21 ejecutadas, 0 fallidas; 32 aserciones, 0 fallidas** | APROBADO | Incluye carpeta SOAP |
| 29 | Matriz funcional propia | script Node (39 aserciones) | 39/39 | **39/39 APROBADO** (tras correcciones) | APROBADO | Ver §3 |
| 30 | Persistencia SQL | estado pre/post `docker compose down` (sin -v) + `up -d` | datos intactos | 7 productos y 3 pedidos idénticos; Flyway sin reaplicar | APROBADO | `down -v` **nunca** ejecutado |
| 31 | Persistencia JWT/API | token pre-reinicio → GET /me | 200 con perfil | 200 "Cliente Demo" | APROBADO | Stateless + llaves estables |
| 32 | Llaves en jars | `grep -a 'keys/*.pem'` en contenedores | private solo en auth; public en user/producto/pedido | Verificado igual (misma pareja, 4 destinos) | APROBADO | — |
| 33 | Secretos en Git | `git status` (rama) | sin `.pem`/`.env` versionados | Sin .pem ni .env en staging | APROBADO | `.gitignore` efectivo |
| 34 | CI/CD estático | revisión de `.github/workflows/ci-cd.yml` | Java 21, verify, push→main, GHCR, mínimo privilegio | Conforme (5 módulos, dueño en minúsculas `camilopaterninaui`) | APROBADO | Ejecución remota pendiente de push autorizado |

## 2. Resultados agregados

| Suite | Total | Aprobados |
|---|---|---|
| Maven (5 módulos) | 69 | 69 |
| Matriz funcional HTTP (script propio) | 39 | 39 |
| Postman (newman: solicitudes) | 21 | 21 |
| Postman (newman: aserciones) | 32 | 32 |

## 3. Fallos encontrados, corregidos y re-verificados

1. **`POST /api/pedidos` devolvía 500** (fallo real de integración detectado por las pruebas funcionales). Evidencia en el log: `HttpClientErrorException$Unauthorized: 401 on GET request for "http://producto-service:8083/api/productos/2"` — al proteger el catálogo, pedido-service consultaba el precio **sin JWT**. Corrección mínima sin romper la seguridad: el controlador captura el `Authorization` del usuario autenticado y `PedidoService` lo **propaga** a producto-service con `RestTemplate.exchange` (la consulta del precio se hace con la identidad de quien pide); se mapea también el rechazo del upstream (401/403 → 502). Re-compilado (13/13 pruebas del módulo), imagen reconstruida y re-ejecutada la matriz: 39/39 ✓. **Este es exactamente el tipo de regresión que la misión pedía detectar: proteger productos sin romper pedidos.**
2. `import ... as ...` en `ProductoEndpoint` (sintaxis inexistente en Java) → corregido al patrón del código original.
3. CommandLineRunner dentro de la clase de aplicación rompía los `@WebMvcTest` → reubicado en `config.DatosInicialesConfig` (semillas intactas).
4. Clases de soporte de prueba en paquete erróneo (`com.unibague.producto.support`) → movidas a `com.unibague.productoservice.support` (ídem pedido).
5. `application.properties` originales habrían pisado la configuración YAML (precedencia de Spring) → eliminados al migrar a `application.yml`.
6. Dos aserciones del propio script de validación usaban `JSON.parse` (que pierde la escala decimal de JS): se verifican ahora sobre el **texto crudo** de la respuesta (`"precio":120000.00`, `"total":255000.00`).
7. La prueba de perfil devolvía 409 en re-corridas → script re-ejecutable (201 nuevo / 409 duplicado, ambos contratos válidos).

## 4. Pendientes sujetos a autorización

| Ítem | Motivo |
|---|---|
| Push de la rama `taller02-evolucion` al remoto | La misión prohíbe publicar sin autorización. El workflow CI/CD ejecutará `verify` y publicará imágenes al fusionar a `main` |
| Limpieza de datos demo (`docker compose down -v`) | Regla de no destrucción. Los datos residuales (usuarios `cliente.demo.*`, productos "Raqueta de tenis" y 3 pedidos) son desechables del entorno académico |

## 5. Conclusión

Las 34 verificaciones ejecutables de la matriz se aprobaron con evidencia verificable, incluidos los requisitos más exigentes del Taller 02: compilación real de los 5 módulos con 69 pruebas, JWT RS256 con propagación de identidad entre microservicios, bases PostgreSQL separadas con Flyway, dinero exacto con BigDecimal de extremo a extremo (JSON + PostgreSQL + SOAP `xs:decimal`), **compatibilidad SOAP intacta** (WSDL y operación por el Gateway), la colección Postman completa en verde y persistencia tras reinicio. La validación remota del workflow queda pendiente exclusivamente del push autorizado.
