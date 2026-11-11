# Proyecto 03 - Requisitos y Matriz de Trazabilidad

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**
Fuente académica: documento de requisitos "Spring Boot 3.0 con microservicios, Docker, Kubernetes, PostgreSQL y Keycloak.pdf" (Proyecto 03).

Este documento distingue cuatro categorías de requisitos y registra las decisiones de diseño adicionales.

## 1. Requisitos expresos del PDF (Proyecto 03)

| # | Requisito expreso del PDF | Estado en Tienda Deportiva v3.0 | Evidencia |
|---|---|---|---|
| R-01 | Spring Boot con microservicios, Docker, Kubernetes, PostgreSQL y Keycloak | ✅ Implementado | Spring Boot 4.0.8 (ver decisión D-01), 6 microservicios, manifiestos k8s, PostgreSQL por servicio, Keycloak |
| R-02 | API Gateway como punto de entrada | ✅ Conservado y extendido | `api-gateway:8080` con rutas a los 6 servicios y `X-Correlation-Id` |
| R-03 | Autenticación, usuarios, productos, pedidos y SOAP conservados | ✅ Conservado | auth-service, user-service, producto-service, pedido-service, SOAP `/ws` |
| R-04 | Keycloak con modelo de identidad consistente (un único emisor JWT) | ✅ Implementado | Keycloak realm `tienda-deportiva`; se eliminó el segundo emisor `taller02-auth` |
| R-05 | Servicio de pagos incorporado | ✅ Implementado | `pago-service:8085` con contratos propios (ver ARCHITECTURE.md) |
| R-06 | Pedido-service en puerto diferente para evitar conflictos | ✅ Implementado | pedido-service en 8084, pago-service en 8085 |
| R-07 | Namespace `proyecto-03` | ✅ Implementado | `k8s/namespace.yaml` |
| R-08 | Deployments de los microservicios | ✅ Implementado | `k8s/*.yaml` (12 deployments: 6 apps + 6 PostgreSQL + keycloak) |
| R-09 | Servicios con comunicación interna (DNS de k8s) | ✅ Implementado | Todos los Services son `ClusterIP`; las apps usan DNS interno (`keycloak:8080`, `user-service:8082`, etc.), **nunca localhost** entre pods |
| R-10 | Keycloak con PostgreSQL independiente | ✅ Implementado | `keycloak-postgres` + `keycloak` en `proyecto-03` |
| R-11 | PostgreSQL independientes por servicio | ✅ Implementado | 6 PostgreSQL (auth no tiene BD propia; delega en Keycloak) |
| R-12 | PersistentVolumeClaims | ✅ Implementado | 6 PVC (una por PostgreSQL) con `storageClassName: hostpath` |
| R-13 | ConfigMaps | ✅ Implementado | `keycloak-realm` (realm JSON) |
| R-14 | Kubernetes Secrets | ✅ Implementado | `tienda-secrets` (creado desde `.k8s-env`, sin revelar valores) |
| R-15 | Readiness y liveness probes | ✅ Implementado | `httpGet` en `/actuator/health` (apps) y `exec`/`tcpSocket` (PostgreSQL/Keycloak) |
| R-16 | Recursos CPU/memoria razonables para Docker Desktop | ✅ Implementado | requests 250m/256Mi, limits 500m/512Mi (apps); keycloak 500m/512Mi - 1000m/1Gi |
| R-17 | Políticas de reinicio y persistencia | ✅ Implementado | `restartPolicy: Always`; PVC con `ReadWriteOnce` y `ReclaimPolicy: Delete` |
| R-18 | OAuth 2.0 / OpenID Connect con Keycloak | ✅ Implementado | Realm, clientes, roles USER/ADMIN, emisión y validación de tokens, autorización por roles |
| R-19 | Integración del auth-service existente | ✅ Implementado | auth-service delega registro/login en Keycloak (único emisor) |
| R-20 | Gestión segura de contraseñas, secretos y llaves | ✅ Implementado | Secretos vía OpenSSL + Kubernetes Secret; nunca en Git |

## 2. Requisitos heredados del Taller 02 (conservados)

| # | Requisito Taller 02 | Estado | Evidencia |
|---|---|---|---|
| T-01 | Java 21 LTS | ✅ | `<java.version>21</java.version>` |
| T-02 | Spring Boot 4.0.8 | ✅ | Ver decisión D-01 (verificación de compatibilidad real) |
| T-03 | POM raíz + módulos multimódulo | ✅ | 6 módulos bajo `microservicios-tienda:3.0.0` |
| T-04 | Spring Cloud Gateway | ✅ | `api-gateway` con `spring-cloud-starter-gateway-server-webflux` |
| T-05 | PostgreSQL independiente por servicio (Database per Service) | ✅ | 4 BD de negocio + Keycloak BD |
| T-06 | Flyway V1 + `ddl-auto: validate` | ✅ | 7 migraciones versionadas + validate |
| T-07 | JWT RS256 | ✅ | Ahora emitido por Keycloak (único emisor); servicios validan vía OIDC/JWKS |
| T-08 | BCrypt + roles USER/ADMIN | ✅ | Keycloak gestiona contraseñas; roles USER/ADMIN en el realm |
| T-09 | DTOs, validación, manejo global de errores | ✅ | DTOs (records), `@Valid`, `GlobalExceptionHandler` en cada servicio |
| T-10 | Swagger/OpenAPI con bearerAuth | ✅ | springdoc 3.0.1 en los 6 servicios |
| T-11 | Docker Compose con healthchecks y volúmenes | ✅ | Conservado como alternativa local |
| T-12 | Dockerfiles multietapa (usuario no-root) | ✅ | 6 Dockerfiles (Maven → JRE, usuario `spring` uid 1001) |
| T-13 | Pruebas automatizadas | ✅ | 70+ pruebas (unitarias, rebanada web, SOAP, servicio) |
| T-14 | Conservar productos, pedidos y SOAP | ✅ | REST + SOAP `/ws` + WSDL + seed de productos |
| T-15 | Puerto del pedido-service diferente (evitar conflictos) | ✅ | pedido-service en 8084 |
| T-16 | Corrección `double` monetario → `BigDecimal` | ✅ | `BigDecimal` + `NUMERIC(12,2)` |
| T-17 | URL de producto-service configurable | ✅ | `PRODUCTO_SERVICE_URL` por entorno (DNS interno en k8s) |

## 3. Funcionalidades originales de Tienda Deportiva (conservadas)

| # | Funcionalidad original | Estado | Evidencia |
|---|---|---|---|
| F-01 | Catálogo de productos (REST) | ✅ | `GET /api/productos`, `GET /api/productos/{id}` |
| F-02 | Crear producto (solo ADMIN) | ✅ | `POST /api/productos` con `@PreAuthorize("hasRole('ADMIN')")` |
| F-03 | Pedidos con consulta de precio a producto-service | ✅ | `POST /api/pedidos?productoId=&cantidad=` |
| F-04 | Servicio SOAP (`/ws`) y WSDL | ✅ | Conservado públicamente (decisión documentada en SECURITY.md) |
| F-05 | Semántica de fault del SOAP | ✅ | Mantenida |
| F-06 | Usuarios demo admin/student | ✅ | Migrados a Keycloak realm `tienda-deportiva` |

## 4. Decisiones adicionales de diseño (no están en el PDF)

| # | Decisión | Justificación | Documentación |
|---|---|---|---|
| D-01 | Usar Spring Boot 4.0.8 (no 3.0) a pesar del título del PDF | Verificación real de compatibilidad en Maven Central y compilación exitosa; cambiar a 3.0 sin analizar impacto sería innecesario | docs/ARCHITECTURE.md, DECISION D-01 |
| D-02 | Keycloak como único emisor JWT (eliminar `taller02-auth`) | Evitar dos emisores JWT contradictorios (regla de arquitectura #5) | docs/SECURITY.md |
| D-03 | Contratos de pago definidos por el equipo | El PDF no especifica contratos de pago; se definen propios y se documentan como tales | docs/ARCHITECTURE.md (Contratos de pago) |
| D-04 | Secreto HMAC para webhook sin integración externa | El PDF menciona un secreto HMAC; se determina su propósito (validar webhooks de pago) pero no se integra con ningún servicio externo real | docs/SECURITY.md (Webhook HMAC) |
| D-05 | SOAP `/ws` mantenido público | No romper clientes SOAP clásicos del proyecto original (decisión documentada del Taller 02) | docs/SECURITY.md |
| D-06 | auth-service sin PostgreSQL propia | La identidad vive en Keycloak; auth-service solo delega | docs/ARCHITECTURE.md |
| D-07 | `storageClassName: hostpath` en PVC | `local-path` no existe en el clúster; disponibles `hostpath` y `standard` | docs/KUBERNETES_DEPLOYMENT.md |
| D-08 | Realm configurado mediante Job (`kcadm.sh`) | La importación de realm vía `start-dev` de Keycloak es inconsistente; se usa un Job de inicialización | docs/KEYCLOAK_SETUP.md |
| D-09 | Pruebas unitarias con emisor de prueba (`test-auth`) | No depende de Keycloak real en el alcance de prueba unitaria; la integración real se valida en Kubernetes | docs/SECURITY.md |

## 5. Diferencias con el ejemplo del profesor

| Aspecto | Ejemplo del profesor (PDF) | Tienda Deportiva v3.0 | Motivo |
|---|---|---|---|
| Spring Boot | 3.0 (título del documento) | 4.0.8 (verificación real de compatibilidad) | Decisión D-01 |
| Emisor JWT | Posiblemente dos emisores | Un único emisor (Keycloak) | Regla de arquitectura #5 |
| Servicio de pagos | Aparece en la arquitectura | Implementado con contratos propios (D-03) | Requisito R-05 |
| Puerto de pedido | Específico del ejemplo | 8084 (evita conflictos con auth 8081/users 8082) | Requisito R-06 |
| Base de datos | Posiblemente compartida | PostgreSQL independiente por servicio | Database per Service (T-05, R-11) |

## Matriz de trazabilidad resumida

| Categoría | Cantidad | Ejemplos |
|---|---|---|
| Requisitos expresos del PDF | 20 | R-01 a R-20 |
| Requisitos heredados del Taller 02 | 17 | T-01 a T-17 |
| Funcionalidades originales de Tienda Deportiva | 6 | F-01 a F-06 |
| Decisiones adicionales de diseño | 9 | D-01 a D-09 |
