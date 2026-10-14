# Tienda Deportiva — Microservicios v2.0 (Taller 02)

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**
Evolución del proyecto de tienda deportiva para cumplir el **Taller 02 v2.0**: Java 21, Spring Boot 4.0.8, Spring Cloud Gateway, JWT RS256, PostgreSQL por servicio, Flyway, DTOs/validación, Swagger, Docker Compose y CI/CD — **conservando el dominio original (productos, pedidos y el servicio SOAP)**.

> 🧪 **Prueba manual en Postman:** sigue la guía paso a paso de [`GUIA_PRUEBAS_POSTMAN.md`](GUIA_PRUEBAS_POSTMAN.md) — URLs, bodies JSON válidos y códigos esperados de todos los endpoints.
>
> ✅ **Reproducibilidad verificada:** este proyecto se probó desde una clonación limpia en una máquina con solo Windows + Docker Desktop + Git (ver `VALIDATION_REPORT.md`).

## Arquitectura

| Componente | Puerto | Responsabilidad | Datos |
|---|---|---|---|
| api-gateway | 8080 | Punto único de entrada, rutas y `X-Correlation-Id` | Sin BD |
| auth-service | 8081 | Registro, login, BCrypt, emisión JWT RS256 (llave privada) | auth_db |
| user-service | 8082 | CRUD de perfiles de usuario | users_db |
| producto-service | 8083 | Catálogo REST **+ SOAP original (/ws)** | productos_db |
| pedido-service | 8084 | Pedidos; consulta el precio a producto-service | pedidos_db |

> **Nota de evolución:** originalmente producto ocupaba 8081 y pedido 8082. El contrato de puertos del Taller 02 asigna 8081/8082 a auth/user, por lo que los servicios de negocio migraron a 8083/8084. Todas las operaciones se conservan y quedan accesibles por el Gateway (`http://localhost:8080/api/...` y `/ws`).

Cada servicio de negocio tiene **su propia base PostgreSQL** (Database per Service) con migraciones **Flyway** y `ddl-auto: validate`.

## Requisitos

### Obligatorios (sin ellos el proyecto no arranca)

| Herramienta | Para qué | Cómo verificar |
|---|---|---|
| **Git** (incluye **Git Bash** y **OpenSSL**) | Clonar y ejecutar el script de llaves (`sh scripts/...` solo funciona en Git Bash / Linux / macOS) | `git --version` |
| **Docker Desktop** con Compose v2 | Construir y ejecutar los 9 contenedores (4 PostgreSQL + 5 aplicaciones) | `docker --version` y `docker compose version` |

> Windows: abre Docker Desktop y espera el mensaje **"Engine running"** antes de continuar (`docker info` debe responder).

### Opcionales (solo para usos específicos)

| Herramienta | Para qué |
|---|---|
| Postman (o newman) | Pruebas manuales/automatizadas de la API (alternativa: `curl`, incluido en Git Bash) |
| Java 21 + Maven 3.9+ | Ejecutar sin contenedores o compilar localmente (los contenedores ya traen su propio JDK) |

## ⚡ Instalación rápida (copiar y pegar en orden)

> Probado en Windows 10/11 con Docker Desktop y **Git Bash** (ejecuta todos los comandos en una ventana de Git Bash).

```bash
# 1. Clonar el repositorio y entrar a la carpeta
git clone https://github.com/CamiloPaterninaUI/MicroserviciosApis2026.git
cd MicroserviciosApis2026

# 2. Verificar requisitos
git --version && docker --version && docker compose version   # y Docker Desktop abierto

# 3. Crear el archivo de variables locales (queda ignorado por Git)
cp .env.example .env

# 4. Generar el par de llaves RSA (necesario para los JWT; solo va a tu máquina)
sh scripts/generate-rsa-keys.sh

# 5. Construir e iniciar los 9 servicios (la primera vez tarda varios minutos)
docker compose up --build -d

# 6. Verificar que todo quedo bien
docker compose ps                                     # 9 servicios Up, BDs (healthy)
curl http://localhost:8080/actuator/health            # {"status":"UP"}
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"Student123!"}' # respuesta con accessToken
```

Si los tres checks del paso 6 responden, el proyecto quedó funcionando. Para probar cada endpoint ve a [`GUIA_PRUEBAS_POSTMAN.md`](GUIA_PRUEBAS_POSTMAN.md).

## 🔑 Llaves RSA: generación, verificación y por qué no están en Git

Los JWT se firman con RSA: **auth-service firma con la llave privada** y user/producto/pedido **validan con la pública** pareja.

**Generarlas (una sola vez, después de clonar):**

```bash
sh scripts/generate-rsa-keys.sh
```

**Comprobar que fueron creadas** (deben existir 5 archivos):

```bash
ls auth-service/src/main/resources/keys/       # private.pem y public.pem
ls user-service/src/main/resources/keys/       # public.pem
ls producto-service/src/main/resources/keys/   # public.pem
ls pedido-service/src/main/resources/keys/     # public.pem
```

Verificación criptográfica de la pareja (opcional):

```bash
openssl rsa -in auth-service/src/main/resources/keys/private.pem -pubout \
  | diff - auth-service/src/main/resources/keys/public.pem && echo "PAREJA COHERENTE"
```

**Por qué no están en Git:** publicar la llave privada permitiría falsificar tokens de cualquier usuario. Están excluidas por `.gitignore` (`keys/*.pem`), así que **cada máquina genera su propio par** con el script (usa OpenSSL, incluido en Git Bash). Sin estas llaves `auth-service` no arranca. Si las regeneras, todos los tokens emitidos antes dejan de ser válidos (los usuarios deben iniciar sesión de nuevo).

## URLs (con el proyecto corriendo)

| Recurso | URL |
|---|---|
| API Gateway (uso normal) | http://localhost:8080 |
| Swagger auth / users / producto / pedido | http://localhost:808X/swagger-ui.html (X = 1..4) |
| WSDL SOAP | http://localhost:8080/ws/productos.wsdl |
| Health | http://localhost:8080/actuator/health (y 8081-8084) |

## Usuarios demo (uso académico)

| Rol | Usuario | Contraseña |
|---|---|---|
| ADMIN | admin | `Admin123!` |
| USER | student | `Student123!` |

Los crea `auth-service` al arrancar (si no existen). Contraseñas con hash BCrypt; nunca en logs ni respuestas.

## Endpoints por el Gateway (:8080)

**Autenticación (público):** `POST /api/auth/register` (201; duplicado 409; inválido 400) · `POST /api/auth/login` (200 con JWT; credenciales erróneas 401).

**Perfiles (JWT):** `POST /api/users` (201) · `GET /api/users/me` (200) · `GET /api/users` (solo ADMIN) · `GET|PUT /api/users/{id}` (propietario/ADMIN) · `DELETE /api/users/{id}` (solo ADMIN).

**Catálogo (JWT):** `GET /api/productos` y `GET /api/productos/{id}` (autenticado; inexistente 404) · `POST /api/productos` (**solo ADMIN**; inválido 400).

**Pedidos (JWT):** `GET /api/pedidos` · `POST /api/pedidos?productoId={id}&cantidad={n}` (total calculado contra producto-service; producto inexistente 404; servicio caído 502; cantidad ≤ 0 400).

**SOAP (público, conservado):** `POST /ws` con `getProductoRequest` · WSDL en `GET /ws/productos.wsdl`.

Bodies JSON válidos y códigos esperados de cada uno: [`GUIA_PRUEBAS_POSTMAN.md`](GUIA_PRUEBAS_POSTMAN.md).

## ▶️ Iniciar, detener y volver a iniciar (conservando los datos)

```bash
docker compose up -d          # iniciar (o retomar) los 9 servicios
docker compose ps             # verificar estado

docker compose down           # DETENER: los contenedores se eliminan pero los
                              # VOLÚMENES PERMANECEN → los datos sobreviven
docker compose up -d          # volver a iniciar: los datos reaparecen solos
                              # (Flyway no reaplica migraciones ya aplicadas)

# docker compose down -v      # ¡PELIGROSO! borra volúmenes y TODOS los datos.
                              # Usar solo para reiniciar el taller desde cero.
```

> Concepto clave: eliminar contenedores no elimina volúmenes. Los datos de las 4 bases sobreviven a cualquier `down` (sin `-v`).

## Postman

**Prueba manual:** sigue la guía paso a paso de [`GUIA_PRUEBAS_POSTMAN.md`](GUIA_PRUEBAS_POSTMAN.md) — URLs, bodies JSON válidos y códigos esperados de todos los endpoints, en el orden correcto.

**Prueba automatizada:** importa `postman/Tienda_Deportiva_v2.postman_collection.json` + `postman/Tienda_Deportiva_v2.postman_environment.json`, selecciona el ambiente "Tienda Deportiva v2.0 - Local" y ejecuta con Runner (guarda tokens e IDs automáticamente). También con newman:

```bash
newman run postman/Tienda_Deportiva_v2.postman_collection.json \
  -e postman/Tienda_Deportiva_v2.postman_environment.json
```

## 🛠️ Solución de problemas

| Problema | Síntoma / causa | Solución |
|---|---|---|
| Docker apagado | `error during connect: ... dockerDesktopLinuxEngine` | Abre **Docker Desktop**, espera "Engine running", verifica con `docker info` y reintenta |
| OpenSSL ausente | `sh: openssl: command not found` al generar llaves | Usa **Git Bash** (incluye OpenSSL). Verifica con `openssl version`. En Linux: `apt install openssl` |
| Puertos ocupados | `bind: address already in use` (8080, 8081-8084 o 5433-5436) | Detén la app que ocupa el puerto, o cambia el puerto externo en `docker-compose.yml` (p. ej. `"9080:8080"`) |
| Error de construcción | `RUN mvn ... returned a non-zero code` (fallo de red al descargar dependencias) | Reintenta `docker compose up --build -d` (Maven reusa lo descargado). Si persiste: `docker builder prune` y reintenta |
| Falta el `.env` | `env file /path/.env not found` | `cp .env.example .env` (sin este paso Compose usa los valores por defecto, pero es mejor crearlo) |
| `auth-service` no arranca | Log: `No fue posible cargar la llave privada RSA` | No ejecutaste el paso 4: `sh scripts/generate-rsa-keys.sh` |
| Base de datos no lista | Logs de PostgreSQL o healthcheck `unhealthy` | `docker compose logs auth-db` (ídem users-db/productos-db/pedidos-db); espera y reintenta |
| Flyway / esquema | `Successfully applied 1 migration` no aparece en logs | `docker compose logs auth-service` y demás; las tablas se crean automáticamente la primera vez |
| JWT inválido / 401 inesperado | Token funcionaba y dejó de hacerlo | Regeneraste las llaves: los tokens anteriores se firmaron con otra llave. Inicia sesión de nuevo |
| 401 en endpoints protegidos | `GET /api/productos` sin token | Es lo esperado: envía `Authorization: Bearer <accessToken>` (ver guía Postman) |
| No recuerdas si hay datos | — | `docker compose exec productos-db psql -U postgres -d productos_db -c "SELECT * FROM producto;"` |
| Reinicio total del taller | Quieres borrar TODO (datos incluidos) | `docker compose down -v && docker compose up --build -d` (irreversible) |

## Ejecución sin Docker (opcional)

Requiere Java 21 + Maven 3.9+ y 4 PostgreSQL locales (o ajustar las variables `*_DB_URL`):

```bash
sh scripts/generate-rsa-keys.sh
mvn -B clean verify                       # 69 pruebas automatizadas
mvn -B -pl producto-service -am spring-boot:run   # BD en 5435 (o PRODUCTOS_DB_URL)
mvn -B -pl pedido-service -am spring-boot:run      # 5436; PRODUCTO_SERVICE_URL apunta a producto
mvn -B -pl auth-service -am spring-boot:run        # 5433
mvn -B -pl user-service -am spring-boot:run        # 5434
mvn -B -pl api-gateway -am spring-boot:run         # 8080
```

## CI/CD

`.github/workflows/ci-cd.yml`: en cada push/PR ejecuta `mvn -B clean verify` (Java 21); en push a `main` construye y publica las 5 imágenes en `ghcr.io/camilopaterninaui/tienda-<servicio>` con `GITHUB_TOKEN`. Las imágenes publicadas **no contienen llaves RSA** (no se versionan): para usarlas en otro entorno hay que inyectar el par de llaves en el despliegue.

## Seguridad

- BCrypt para contraseñas; nunca en respuestas REST.
- JWT RS256: la llave privada vive solo en auth-service; user/producto/pedido validan con la pública (issuer `taller02-auth`).
- Llaves `.pem` y `.env` fuera de Git (`.gitignore`).
- El SOAP `/ws/**` se mantiene público para preservar la interoperabilidad clásica del proyecto original (decisión documentada en `IMPLEMENTATION_NOTES.md`).

## Documentación

- `GUIA_PRUEBAS_POSTMAN.md` — guía de pruebas manuales en Postman para cualquiera que levante el proyecto.
- `IMPLEMENTATION_NOTES.md` — matriz de auditoría y todas las correcciones documentadas.
- `VALIDATION_REPORT.md` — evidencia de cada verificación ejecutada.
- `FINAL_TEST_REPORT.md` — informe final de la validación integral.
