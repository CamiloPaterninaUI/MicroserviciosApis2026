# Tienda Deportiva — Microservicios v2.0 (Taller 02)

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**
Evolución del proyecto de tienda deportiva para cumplir el **Taller 02 v2.0**: Java 21, Spring Boot 4.0.8, Spring Cloud Gateway, JWT RS256, PostgreSQL por servicio, Flyway, DTOs/validación, Swagger, Docker Compose y CI/CD — **conservando el dominio original (productos, pedidos y el servicio SOAP)**.

## Arquitectura

| Componente | Puerto | Responsabilidad | Datos |
|---|---|---|---|
| api-gateway | 8080 | Punto único de entrada, rutas y `X-Correlation-Id` | Sin BD |
| auth-service | 8081 | Registro, login, BCrypt, emisión JWT RS256 (llave privada) | auth_db |
| user-service | 8082 | CRUD de perfiles de usuario | users_db |
| producto-service | 8083 | Catálogo REST **+ SOAP original (/ws)** | productos_db |
| pedido-service | 8084 | Pedidos; consulta el precio a producto-service | pedidos_db |

> **Nota de evolución:** originalmente producto ocupaba 8081 y pedido 8082. El contrato de puertos del Taller 02 asigna 8081/8082 a auth/user, por lo que los servicios de negocio migraron a 8083/8084. Todas las operaciones se conservan y además quedan accesibles por el Gateway (`http://localhost:8080/api/...` y `/ws`).

Cada servicio de negocio tiene **su propia base PostgreSQL** (Database per Service) con migraciones **Flyway** y `ddl-auto: validate`.

## Requisitos previos

- Docker Desktop (Compose v2)
- Git (incluye OpenSSL en Git Bash)
- Opcional: Java 21 + Maven 3.9+ para ejecución sin contenedores

## Preparación (primera vez)

```bash
cp .env.example .env
sh scripts/generate-rsa-keys.sh   # genera el par RSA; la privada SOLO va a auth-service
```

## Arranque

```bash
docker compose up --build -d
docker compose ps                  # 9 servicios: 4 PostgreSQL + 5 aplicaciones
curl http://localhost:8080/actuator/health   # {"status":"UP"} (también 8081-8084)
```

## URLs

| Recurso | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Swagger auth | http://localhost:8081/swagger-ui.html |
| Swagger users | http://localhost:8082/swagger-ui.html |
| Swagger producto | http://localhost:8083/swagger-ui.html |
| Swagger pedido | http://localhost:8084/swagger-ui.html |
| WSDL SOAP | http://localhost:8080/ws/productos.wsdl |
| Health | http://localhost:8080/actuator/health |

## Usuarios demo (uso académico)

| Rol | Usuario | Contraseña |
|---|---|---|
| ADMIN | admin | `Admin123!` |
| USER | student | `Student123!` |

## Endpoints por el Gateway (:8080)

**Autenticación (público):**
- `POST /api/auth/register` — crea credencial USER (201); duplicados → 409; inválido → 400
- `POST /api/auth/login` — devuelve JWT Bearer RS256 (200); credenciales erróneas → 401

**Perfiles (JWT):** `POST /api/users` (201), `GET /api/users/me` (200), `GET /api/users` (solo ADMIN), `GET|PUT /api/users/{id}` (propietario/ADMIN), `DELETE /api/users/{id}` (solo ADMIN).

**Catálogo (JWT):** `GET /api/productos` y `GET /api/productos/{id}` (autenticado; inexistente → 404), `POST /api/productos` (**solo ADMIN**, 201; inválido → 400).

**Pedidos (JWT):** `GET /api/pedidos` (200) y `POST /api/pedidos?productoId={id}&cantidad={n}` (200, calcula el total contra producto-service; producto inexistente → 404; producto-service caído → 502; cantidad ≤ 0 → 400).

**SOAP (público, conservado):** `POST /ws` con `getProductoRequest` → `getProductoResponse`; WSDL en `GET /ws/productos.wsdl`.

## Prueba rápida con curl

```bash
# Login y token (nunca se imprimen tokens completos en los reportes)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"Student123!"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# Catálogo protegido
curl -i http://localhost:8080/api/productos -H "Authorization: Bearer $TOKEN"

# Crear un pedido (el total lo calcula pedido-service con BigDecimal)
curl -i -X POST "http://localhost:8080/api/pedidos?productoId=2&cantidad=3" \
  -H "Authorization: Bearer $TOKEN"

# SOAP original por el Gateway
curl -s http://localhost:8080/ws/productos.wsdl
curl -s -X POST http://localhost:8080/ws \
  -H "Content-Type: text/xml; charset=utf-8" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:prod="http://unibague.edu.co/productos"><soapenv:Header/><soapenv:Body><prod:getProductoRequest><prod:id>1</prod:id></prod:getProductoRequest></soapenv:Body></soapenv:Envelope>'
```

## Postman

**Prueba manual:** sigue la guía paso a paso de [`GUIA_PRUEBAS_POSTMAN.md`](GUIA_PRUEBAS_POSTMAN.md) — URLs, bodies JSON válidos y códigos esperados de todos los endpoints.

**Prueba automatizada:** importe `postman/Tienda_Deportiva_v2.postman_collection.json` y `postman/Tienda_Deportiva_v2.postman_environment.json`, seleccione el ambiente "Tienda Deportiva v2.0 - Local" y ejecute la colección en orden con Runner (también funciona con `newman`):

```bash
newman run postman/Tienda_Deportiva_v2.postman_collection.json -e postman/Tienda_Deportiva_v2.postman_environment.json
```

## Persistencia

```bash
docker volume ls                          # 4 volúmenes: auth/users/productos/pedidos
docker compose down                       # los datos PERMANECEN
docker compose up -d                      # los datos reaparecen
# docker compose down -v                  # ¡SOLO si desea reiniciar por completo (borra datos)!
```

## Ejecución sin Docker

```bash
sh scripts/generate-rsa-keys.sh
mvn -B clean verify                       # 69 pruebas automatizadas
mvn -B -pl producto-service -am spring-boot:run   # requiere PostgreSQL en 5435 (o PRODUCTOS_DB_URL)
mvn -B -pl pedido-service -am spring-boot:run      # 5436; PRODUCTO_SERVICE_URL apunta a producto
mvn -B -pl auth-service -am spring-boot:run        # 5433
mvn -B -pl user-service -am spring-boot:run        # 5434
mvn -B -pl api-gateway -am spring-boot:run        # 8080
```

## CI/CD

`.github/workflows/ci-cd.yml`: en cada push/PR ejecuta `mvn -B clean verify` (Java 21); en push a `main` construye y publica las 5 imágenes en `ghcr.io/camilopaterninaui/tienda-<servicio>` con `GITHUB_TOKEN`.

## Seguridad

- BCrypt para contraseñas; nunca en respuestas REST.
- JWT RS256: la llave privada vive solo en auth-service; user/producto/pedido validan con la pública (issuer `taller02-auth`).
- Llaves `.pem` y `.env` fuera de Git.
- El SOAP `/ws/**` se mantiene público para preservar la interoperabilidad clásica del proyecto original (decisión documentada en `IMPLEMENTATION_NOTES.md`).

## Documentación

- `GUIA_PRUEBAS_POSTMAN.md` — guía de pruebas manuales en Postman para cualquiera que levante el proyecto.
- `IMPLEMENTATION_NOTES.md` — matriz de auditoría y todas las correcciones documentadas.
- `VALIDATION_REPORT.md` — evidencia de cada verificación ejecutada.
- `FINAL_TEST_REPORT.md` — informe final de la validación integral.
