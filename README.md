# Tienda Deportiva — Microservicios v3.0 (Proyecto 03 - Kubernetes + Keycloak)

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**
Evolución de la Tienda Deportiva al **Proyecto 03** del documento de requisitos (Spring Boot 3.0 con microservicios, Docker, Kubernetes, PostgreSQL y Keycloak). Conserva las funcionalidades actuales de autenticación, usuarios, productos, pedidos y SOAP, e incorpora **Kubernetes**, **Keycloak (OAuth 2.0 / OpenID Connect)** y el nuevo servicio de **pagos**.

> ⚠️ **Estado:** Rama de trabajo `proyecto03-kubernetes-keycloak`. **No hacer push, merge ni publicar imágenes nuevas sin autorización.**

## Arquitectura (Proyecto 03)

| Componente | Puerto | Responsabilidad | Datos |
|---|---|---|---|
| api-gateway | 8080 | Punto único de entrada, rutas y `X-Correlation-Id` | Sin BD |
| auth-service | 8081 | Registro/login delegado en Keycloak (único emisor JWT) | Sin BD propia |
| user-service | 8082 | CRUD de perfiles de usuario | users_db |
| producto-service | 8083 | Catálogo REST + SOAP original (`/ws`) | productos_db |
| pedido-service | 8084 | Pedidos; consulta precio a producto-service | pedidos_db |
| pago-service | 8085 | Pagos del Proyecto 03 (contratos propios) | pagos_db |
| keycloak | 8080 | Proveedor de identidad OAuth 2.0 / OIDC (emisor único) | keycloak |

Cada servicio de negocio tiene **su propia base PostgreSQL** (Database per Service) con migraciones Flyway y `ddl-auto: validate`. Keycloak también usa su PostgreSQL dedicada.

### Puertos (nota de evolución)
El contrato del Proyecto 03 asigna `auth-service:8081` y `user-service:8082`, que en la tienda original ocupaban producto y pedido. Se conserva el contrato del taller: auth → 8081, users → 8082, producto → 8083, pedido → 8084 y el nuevo pago → **8085** (para evitar conflictos). Ninguna operación se pierde: todo queda accesible por el Gateway 8080.

## Requisitos del sistema

| Herramienta | Para qué | Verificar |
|---|---|---|
| Git + Git Bash + OpenSSL | Clonar, generar secretos, ejecutar scripts | `git --version`, `openssl version` |
| Docker Desktop con Compose v2 | Construir imágenes y ejecutar localmente | `docker --version`, `docker compose version` |
| Kubernetes (Docker Desktop) | Despliegue en clúster local | `kubectl cluster-info` |
| Java 21 + Maven 3.9+ | Compilar y probar sin contenedores | `java -version`, `mvn -version` |
| kubectl | Gestionar el clúster | `kubectl version --client` |

> Windows: abre Docker Desktop y habilita **Kubernetes** (Settings → Kubernetes → Enable). Espera a que `kubectl cluster-info` responda y el nodo esté `Ready`.

## Instalación desde cero (clonación limpia)

```bash
# 1. Clonar y entrar (rama de trabajo del Proyecto 03)
git clone https://github.com/CamiloPaterninaUI/MicroserviciosApis2026.git
cd MicroserviciosApis2026
git checkout proyecto03-kubernetes-keycloak

# 2. Verificar requisitos
git --version && docker --version && docker compose version && kubectl version --client

# 3. Variables locales (ignorado por Git)
cp .env.example .env

# 4. Generar TODOS los secretos mediante OpenSSL (se escriben en .k8s-env, 600, ignorado por Git)
#    Nunca se muestra el contenido de los secretos por terminal.
sh scripts/generate-rsa-keys.sh

# 5. Construir las 6 imágenes Docker (localmente, sin publicar)
sh scripts/build-images.sh

# 6. Desplegar en Kubernetes (namespace proyecto-03)
sh scripts/k8s-setup.sh

# 7. Verificar estado
kubectl get pods -n proyecto-03
kubectl get services -n proyecto-03
kubectl get pvc -n proyecto-03

# 8. Exponer el Gateway y probar
kubectl port-forward svc/api-gateway 8080:8080 -n proyecto-03
# En otra terminal:
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

Los scripts facilitan un despliegue reproducible desde una clonación nueva:
- `scripts/generate-rsa-keys.sh` — genera secretos con OpenSSL (adaptado para Keycloak; ya no genera llaves RSA JWT porque Keycloak es el único emisor).
- `scripts/build-images.sh` — construye las 6 imágenes `tienda-deportiva/<servicio>:3.0.0`.
- `scripts/k8s-setup.sh` — aplica manifiestos, crea el Secret `tienda-secrets` desde `.k8s-env` y espera los rollouts.

## Seguridad (OAuth 2.0 / OpenID Connect con Keycloak)

- **Realm:** `tienda-deportiva` (único emisor JWT; se eliminó el segundo emisor `taller02-auth`).
- **Clientes:** `tienda-auth` (público, password grant para auth-service) y `tienda-admin` (administrativo).
- **Roles:** `USER` y `ADMIN` (nivel de realm).
- **Emisión y validación:** Keycloak expide los JWT; auth-service solo delega (registro/login). Los servicios de negocio validan mediante descubrimiento OIDC (JWKS de Keycloak).
- **Autorización por roles:** `USER`/`ADMIN` en perfiles, productos, pedidos y pagos.
- **Secretos:** generados con OpenSSL, inyectados vía Kubernetes Secret `tienda-secrets`; **nunca en Git ni en logs** (`.k8s-env` está en `.gitignore` y tiene permisos 600).

## Endpoints (Gateway :8080)

- **Autenticación (público):** `POST /api/auth/register`, `POST /api/auth/login`.
- **Perfiles (JWT):** `POST /api/users`, `GET /api/users/me`, `GET /api/users` (ADMIN), `GET|PUT /api/users/{id}`, `DELETE /api/users/{id}` (ADMIN).
- **Catálogo (JWT):** `GET /api/productos`, `GET /api/productos/{id}`; `POST /api/productos` (solo ADMIN).
- **Pedidos (JWT):** `GET /api/pedidos`, `POST /api/pedidos?productoId={id}&cantidad={n}`.
- **Pagos (JWT):** `POST /api/pagos`, `GET /api/pagos/{id}`, `GET /api/pagos/pedido/{id}`, `POST /api/pagos/{id}/estado` (ADMIN).
- **SOAP (público, conservado):** `POST /ws` y WSDL en `GET /ws/productos.wsdl`.

## Documentación del Proyecto 03

| Documento | Contenido |
|---|---|
| `docs/PROYECTO_03_REQUIREMENTS.md` | Requisitos del PDF, heredados del Taller 02, funcionales originales y decisiones de diseño |
| `docs/ARCHITECTURE.md` | Arquitectura, contratos de pago, diferencias con el ejemplo del profesor |
| `docs/KEYCLOAK_SETUP.md` | Realm, clientes, roles, tokens, HMAC del webhook |
| `docs/KUBERNETES_DEPLOYMENT.md` | Manifiestos, namespace, PVC, ConfigMaps, Secrets, probes, recursos |
| `docs/SECURITY.md` | Modelo de identidad, secretos, contraseñas, llaves, webhook HMAC |
| `VALIDATION_REPORT_PROYECTO03.md` | Evidencia de cada verificación ejecutada |
| `FINAL_TEST_REPORT_PROYECTO03.md` | Informe final de validación integral |

## Comandos útiles (Kubernetes)

```bash
kubectl get all -n proyecto-03
kubectl get pvc -n proyecto-03
kubectl get secret tienda-secrets -n proyecto-03   # solo metadatos, nunca el valor
kubectl port-forward svc/api-gateway 8080:8080 -n proyecto-03
kubectl logs -n proyecto-03 deployment/<servicio>
kubectl rollout status deployment/<servicio> -n proyecto-03
```

## Seguridad y credenciales

- Nunca incluir credenciales reales en Git, manifiestos ni reportes.
- Los secretos se generan con OpenSSL (`scripts/generate-rsa-keys.sh`) y se guardan en `.k8s-env` (ignorado por Git, permisos 600).
- El secreto HMAC del webhook de pagos se define en `docs/SECURITY.md` (contrato propio, sin integración externa real).

## Pruebas

```bash
mvn -B clean verify          # 6 módulos, todas las pruebas pasan
```

## Créditos

Evolución del proyecto Tienda Deportiva (Taller 02) al Proyecto 03 (Universidad de Ibagué). Grupo 06.
