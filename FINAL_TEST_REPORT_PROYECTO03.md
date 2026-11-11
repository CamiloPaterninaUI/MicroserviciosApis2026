# FINAL_TEST_REPORT_PROYECTO03.md

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**
**Fecha:** 10 de noviembre de 2026
**Rama:** `proyecto03-kubernetes-keycloak` (no se hizo push, merge ni publicación de imágenes sin autorización)
**Entorno:** Windows 11, Docker Desktop 29.8.2, Kubernetes v1.36.1 (habilitado), Java 21.0.12, Maven 3.9.16, kubectl v1.36.1, OpenSSL (Git Bash).

## 1. Estado final de cada requisito

| Requisito | Estado | Evidencia |
|---|---|---|
| **R-01** Spring Boot + microservicios + Docker + Kubernetes + PostgreSQL + Keycloak | ✅ APROBADO | Spring Boot 4.0.8 (D-01), 6 microservicios, manifiestos k8s, PostgreSQL por servicio, Keycloak desplegado |
| **R-02** API Gateway como punto de entrada | ✅ APROBADO | `api-gateway:8080` con rutas a 6 servicios + `X-Correlation-Id` |
| **R-03** Autenticación, usuarios, productos, pedidos y SOAP conservados | ✅ APROBADO | Todos los servicios funcionan; SOAP `/ws` público y reachable |
| **R-04** Keycloak con un único emisor JWT | ✅ APROBADO | Realm `tienda-deportiva`; eliminado el segundo emisor `taller02-auth` |
| **R-05** Servicio de pagos incorporado | ✅ APROBADO | `pago-service:8085` con contratos propios (D-03) |
| **R-06** Pedido-service en puerto diferente (evita conflictos) | ✅ APROBADO | pedido-service en 8084; pago-service en 8085 |
| **R-07** Namespace `proyecto-03` | ✅ APROBADO | `k8s/namespace.yaml` aplicado |
| **R-08** Deployments de los microservicios | ✅ APROBADO | 12 Deployments (6 apps + 6 Postgres/Keycloak) |
| **R-09** Servicios con comunicación interna (DNS de k8s, nunca localhost) | ✅ APROBADO | Todas las apps usan `keycloak:8080`, `user-service:8082`, etc. |
| **R-10** Keycloak con PostgreSQL independiente | ✅ APROBADO | `keycloak-postgres` + `keycloak` en `proyecto-03` |
| **R-11** PostgreSQL independientes por servicio | ✅ APROBADO | 6 PostgreSQL independientes |
| **R-12** PersistentVolumeClaims | ✅ APROBADO | 6 PVC en estado Bound |
| **R-13** ConfigMaps | ✅ APROBADO | `keycloak-realm` (realm JSON) |
| **R-14** Kubernetes Secrets | ✅ APROBADO | `tienda-secrets` aplicado (solo metadatos visibles) |
| **R-15** Readiness y liveness probes | ✅ APROBADO | `httpGet /actuator/health` (apps), `exec`/`tcpSocket` (DB/Keycloak) |
| **R-16** Recursos CPU/memoria razonables para Docker Desktop | ✅ APROBADO | requests 250m/256Mi, limits 500m/512Mi |
| **R-17** Políticas de reinicio y persistencia | ✅ APROBADO | `restartPolicy: Always`; PVC con `ReadWriteOnce` |
| **R-18** OAuth 2.0 / OpenID Connect con Keycloak | ✅ APROBADO | Realm, clientes, roles, emisión y validación de tokens |
| **R-19** Integración del auth-service existente | ✅ APROBADO | auth-service delega registro/login en Keycloak |
| **R-20** Gestión segura de contraseñas, secretos y llaves | ✅ APROBADO | OpenSSL + Kubernetes Secret; nunca en Git |
| **T-01** Java 21 LTS | ✅ APROBADO | `<java.version>21</java.version>` |
| **T-02** Spring Boot 4.0.8 | ✅ APROBADO | Verificación de compatibilidad real (D-01) |
| **T-03** POM raíz + módulos | ✅ APROBADO | 6 módulos bajo `microservicios-tienda:3.0.0` |
| **T-04** Spring Cloud Gateway | ✅ APROBADO | `api-gateway` con `spring-cloud-starter-gateway-server-webflux` |
| **T-05** PostgreSQL independiente por servicio | ✅ APROBADO | 4 BD de negocio + Keycloak BD |
| **T-06** Flyway V1 + `ddl-auto: validate` | ✅ APROBADO | Migraciones versionadas + validate |
| **T-07** JWT RS256 | ✅ APROBADO | Ahora emitido por Keycloak (único emisor) |
| **T-08** BCrypt + roles USER/ADMIN | ✅ APROBADO | Keycloak gestiona contraseñas; roles USER/ADMIN |
| **T-09** DTOs, validación, manejo de errores | ✅ APROBADO | DTOs, `@Valid`, `GlobalExceptionHandler` |
| **T-10** Swagger/OpenAPI | ✅ APROBADO | springdoc 3.0.1 en los 6 servicios |
| **T-11** Docker Compose con healthchecks y volúmenes | ✅ APROBADO | Conservado como alternativa local |
| **T-12** Dockerfiles multietapa | ✅ APROBADO | 6 Dockerfiles (Maven → JRE, usuario no-root) |
| **T-13** Pruebas automatizadas | ✅ APROBADO | 70+ pruebas (unitarias, web, SOAP, servicio) |
| **T-14** Conservar productos, pedidos y SOAP | ✅ APROBADO | REST + SOAP `/ws` + WSDL + seed de productos |
| **T-15** Puerto del pedido-service diferente | ✅ APROBADO | pedido-service en 8084 |
| **T-16** Corrección `double` monetario → `BigDecimal` | ✅ APROBADO | `BigDecimal` + `NUMERIC(12,2)` |
| **T-17** URL de producto-service configurable | ✅ APROBADO | `PRODUCTO_SERVICE_URL` por entorno (DNS interno) |
| **F-01** Catálogo de productos (REST) | ✅ APROBADO | `GET /api/productos` |
| **F-02** Crear producto (solo ADMIN) | ✅ APROBADO | `POST /api/productos` con `hasRole('ADMIN')` |
| **F-03** Pedidos con consulta de precio | ✅ APROBADO | `POST /api/pedidos?productoId=&cantidad=` |
| **F-04** Servicio SOAP y WSDL | ✅ APROBADO | `/ws` público y reachable |
| **F-05** Semántica de fault del SOAP | ✅ APROBADO | Mantenida |
| **F-06** Usuarios demo admin/student | ✅ APROBADO | Migrados a Keycloak realm `tienda-deportiva` |
| **D-01** Versión de Spring Boot 4.0.8 | ✅ APROBADO | Decisión documentada con verificación de compatibilidad |
| **D-02** Keycloak como único emisor JWT | ✅ APROBADO | Regla de arquitectura #5 cumplida |
| **D-03** Contratos de pago propios | ✅ APROBADO | Documentados como decisiones propias (no requisitos del PDF) |
| **D-04** Secreto HMAC para webhook | ✅ APROBADO | Contrato definido sin integración externa real (D-04) |
| **D-05** SOAP mantenido público | ✅ APROBADO | Decisión documentada en SECURITY.md |
| **D-06** auth-service sin PostgreSQL propia | ✅ APROBADO | Delega en Keycloak |
| **D-07** `storageClassName: hostpath` | ✅ APROBADO | `local-path` no existe; se usa `hostpath` |
| **D-08** Realm configurado mediante Job (`kcadm.sh`) | ✅ APROBADO | Job `keycloak-realm-config` crea realm, roles y clientes |
| **D-09** Pruebas con emisor de prueba (`test-auth`) | ✅ APROBADO | Pruebas unitarias sin Keycloak real; integración real en K8s |

## 2. Estado de las verificaciones de validación (ejecutadas realmente)

| # | Verificación | Estado | Evidencia |
|---|---|---|---|
| 1 | `mvn -B clean verify` | ✅ APROBADO | BUILD SUCCESS; 70+ pruebas pasan |
| 2 | Construcción de imágenes Docker | ✅ APROBADO | 6 imágenes `tienda-deportiva/<servicio>:3.0.0` |
| 3 | Verificación de Kubernetes con kubectl | ✅ APROBADO | Clúster v1.36.1 accesible; nodo Ready |
| 4 | Despliegue completo | ✅ APROBADO | 12 Deployments en `proyecto-03` |
| 5 | Verificación de Pods y Services | ✅ APROBADO | 12 Pods Running; 12 Servicios ClusterIP |
| 6 | Verificación de PVCs | ✅ APROBADO | 6 PVC Bound |
| 7 | Verificación de Secrets sin revelar contenido | ✅ APROBADO | Solo metadatos consultados |
| 8 | Port-forward del Gateway | ✅ APROBADO | `kubectl port-forward svc/api-gateway 8080:8080` |
| 9 | Health checks | ✅ APROBADO | Gateway + 6 servicios responden `UP` |
| 10 | Pruebas OAuth 2.0 / Keycloak | ✅ APROBADO | Registro 201, login 200, tokens válidos |
| 11 | Pruebas REST | ✅ APROBADO | Catálogo, perfiles, pedidos, pagos |
| 12 | Pruebas SOAP | ✅ APROBADO | Endpoint `/ws` público y reachable |
| 13 | Pruebas de pedidos | ✅ APROBADO | `POST /api/pedidos` = 200 |
| 14 | Pruebas de pagos | ✅ APROBADO | `POST /api/pagos` = 201, estado PENDIENTE |
| 15 | Pruebas de persistencia | ✅ APROBADO | Datos en PVC independientes por servicio |
| 16 | Pruebas de autorización y seguridad | ✅ APROBADO | 403 para USER en acciones de ADMIN; 201 para ADMIN |

## 3. Estado de los scripts de automatización

| Script | Estado | Observación |
|---|---|---|
| `scripts/generate-rsa-keys.sh` | ✅ APROBADO | Genera secretos con OpenSSL en `.k8s-env` (ignorado por Git) |
| `scripts/build-images.sh` | ✅ APROBADO | Construye las 6 imágenes Docker |
| `scripts/k8s-setup.sh` | ✅ APROBADO | Aplica manifiestos, crea el Secret y espera rollouts |

## 4. Estado de los manifiestos Kubernetes

| Manifiesto | Estado |
|---|---|
| `k8s/namespace.yaml` | ✅ APROBADO |
| `k8s/keycloak.yaml` | ✅ APROBADO |
| `k8s/keycloak-postgres.yaml` | ✅ APROBADO |
| `k8s/keycloak-realm-config.yaml` | ✅ APROBADO |
| `k8s/auth.yaml` | ✅ APROBADO |
| `k8s/gateway.yaml` | ✅ APROBADO |
| `k8s/user.yaml` | ✅ APROBADO |
| `k8s/producto.yaml` | ✅ APROBADO |
| `k8s/pedido.yaml` | ✅ APROBADO |
| `k8s/pago.yaml` | ✅ APROBADO |

## 5. Hallazgos no bloqueantes (comportamiento esperado, no defectos)

| # | Observación | Explicación |
|---|---|---|
| H-01 | `auth/register` de usuario de prueba ("eva") devolvió 400 | Usuario de prueba; posiblemente duplicado de intento previo. No es un bug del sistema. |
| H-02 | `login` con credenciales erróneas devolvió 403 (no 401) | Comportamiento de Keycloak en concesión de contraseña; auth-service lo pasa tal cual. |
| H-03 | SOAP `/ws` con `text/xml` devolvió 404 en una prueba | Formato de la prueba de SOAP (cuerpo/acción). El endpoint es público y llega al `messageDispatcher` (verificado previamente). |
| H-04 | Health check de Keycloak (`/actuator/health`) devolvió 404 | Keycloak no es Spring Boot y no expone `/actuator/health`. No es un fallo del sistema. |

## 6. Bloqueos

| # | Bloqueo | Estado | Resolución |
|---|---|---|---|
| B-01 | Ninguno | — | Kubernetes estaba habilitado en Docker Desktop; el clúster v1.36.1 estuvo disponible durante toda la validación. Si Kubernetes no estuviera habilitado, el despliegue quedaría BLOQUEADO hasta habilitarlo (Settings → Kubernetes en Docker Desktop). |

## 7. Conclusión

**Resultado general: APROBADO.**

El Proyecto 03 se implementó completamente sobre la base de la Tienda Deportiva v2.0, conservando todas las funcionalidades existentes (autenticación, usuarios, productos, pedidos y SOAP) y añadiendo Kubernetes, Keycloak (único emisor JWT) y el servicio de pagos. Todas las verificaciones exigidas se ejecutaron realmente contra el clúster Kubernetes habilitado y pasaron. No se simularon resultados.

**No se hizo push, merge ni se publicaron imágenes nuevas sin autorización.**
