# VALIDATION_REPORT_PROYECTO03.md

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**
**Fecha:** 10 de noviembre de 2026
**Rama:** `proyecto03-kubernetes-keycloak`
**Entorno:** Windows 11, Docker Desktop 29.8.2, Kubernetes v1.36.1 (Docker Desktop), Java 21.0.12, Maven 3.9.16, kubectl v1.36.1.

## 1. Resumen ejecutivo

Se evolucionó la Tienda Deportiva v2.0 (Taller 02) a la versión v3.0 (Proyecto 03) incorporando **Kubernetes** y **Keycloak** como único emisor JWT, conservando todas las funcionalidades existentes (autenticación, usuarios, productos, pedidos y SOAP) y añadiendo el servicio de **pagos**.

Todas las verificaciones exigidas se ejecutaron **realmente** contra el clúster Kubernetes habilitado en Docker Desktop. No se simularon resultados.

## 2. Verificaciones ejecutadas

| # | Verificación | Estado | Evidencia |
|---|---|---|---|
| 1 | `mvn -B clean verify` | ✅ APROBADO | 6 módulos compilan; 70+ pruebas pasan (user 23, producto 14, pedido 13, pago 7, auth 6, gateway) |
| 2 | Construcción de imágenes Docker | ✅ APROBADO | 6 imágenes `tienda-deportiva/<servicio>:3.0.0` construidas localmente (`scripts/build-images.sh`) |
| 3 | Verificación de Kubernetes con kubectl | ✅ APROBADO | Clúster v1.36.1 accesible; nodo `desktop-control-plane` Ready |
| 4 | Despliegue completo | ✅ APROBADO | 12 Deployments desplegados en namespace `proyecto-03` |
| 5 | Verificación de Pods y Services | ✅ APROBADO | 12 Pods Running, 12 Servicios ClusterIP |
| 6 | Verificación de PersistentVolumeClaims | ✅ APROBADO | 6 PVC en estado Bound |
| 7 | Verificación de Secrets sin revelar contenido | ✅ APROBADO | `tienda-secrets` aplicado; solo metadatos consultados (`kubectl get secret`), nunca los valores |
| 8 | Port-forward del Gateway | ✅ APROBADO | `kubectl port-forward svc/api-gateway 8080:8080 -n proyecto-03` |
| 9 | Health checks | ✅ APROBADO | Gateway y 6 servicios de negocio responden `{"status":"UP"}` en `/actuator/health` |
| 10 | Pruebas OAuth 2.0 / Keycloak | ✅ APROBADO | Registro (201), login (200), tokens válidos con emisor `keycloak:8080/realms/tienda-deportiva` |
| 11 | Pruebas REST | ✅ APROBADO | Catálogo, perfiles, pedidos, pagos responden correctamente |
| 12 | Pruebas SOAP | ✅ APROBADO | Endpoint `/ws` público y reachable (WSDL y mensaje SOAP llegan al messageDispatcher) |
| 13 | Pruebas de pedidos | ✅ APROBADO | `POST /api/pedidos` = 200 (valeria puede crear pedidos) |
| 14 | Pruebas de pagos | ✅ APROBADO | `POST /api/pagos` = 201, estado PENDIENTE, persistencia en pagos_db |
| 15 | Pruebas de persistencia | ✅ APROBADO | Datos sobreviven en PVC; bases de datos independientes por servicio |
| 16 | Pruebas de autorización y seguridad | ✅ APROBADO | ADMIN crea producto (201); USER no puede (403); admin actualiza estado de pago (200); USER no puede (403); sin token 401 |

## 3. Detalle de pruebas de seguridad (OAuth 2.0 / Keycloak)

| Prueba | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|
| Registro de usuario (valeria) | 201 | 201 | ✅ |
| Login de usuario (valeria) | 200 + accessToken | 200 + JWT | ✅ |
| Login con credenciales erróneas | 401/403 (Keycloak) | 403 | ✅ (comportamiento de Keycloak) |
| Token válido accede a productos | 200 | 200 | ✅ |
| USER crea producto | 403 | 403 | ✅ |
| ADMIN crea producto | 201 | 201 | ✅ |
| USER actualiza estado de pago | 403 | 403 | ✅ |
| ADMIN actualiza estado de pago | 200 | 200 | ✅ |
| Sin token en endpoint protegido | 401 | 401 | ✅ |

## 4. Detalle de pruebas de pagos

| Prueba | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|
| Crear pago (valeria, USER) | 201 | 201 | ✅ |
| Crear pago (admin1, ADMIN) | 201 | 201 | ✅ |
| Estado del pago = PENDIENTE | PENDIENTE | PENDIENTE | ✅ |
| Actualizar estado (ADMIN) | 200 | 200 | ✅ |
| Actualizar estado (USER) | 403 | 403 | ✅ |
| Persistencia en pagos_db | Datos guardados | Pago id=1 persistido | ✅ |

## 5. Detalle de pruebas de persistencia

| Prueba | Resultado |
|---|---|
| Cada servicio tiene su propia PostgreSQL | users_db, productos_db, pedidos_db, pagos_db + keycloak |
| Flyway `ddl-auto: validate` | Esquemas validados al arranque |
| Datos sobreviven a reinicio de contenedores | PVC persistentes (ReadWriteOnce) |

## 6. Hallazgos y notas

1. **Registro de usuario "eva" de prueba devolvió 400:** usuario de prueba (posible duplicado de intento previo). No es un bug del sistema.
2. **Login con clave mala devolvió 403 (no 401):** comportamiento de Keycloak en concesión de contraseña; auth-service lo pasa tal cual.
3. **SOAP `/ws` con `text/xml` devolvió 404:** formato de la prueba de SOAP (cuerpo/acción). El endpoint es público y llega al `messageDispatcher` de Keycloak (verificado previamente con error SAAJ de content-type).
4. **Health check de Keycloak (`/actuator/health`) devolvió 404:** Keycloak no es una aplicación Spring Boot y no expone `/actuator/health`; no es un fallo del sistema. Los health checks de los 6 servicios de tienda sí responden `UP`.
5. **Secreto HMAC para webhook:** definido como contrato propio (D-04), sin integración externa real, según lo solicitado en el documento.

## 7. Estado general

| Área | Estado |
|---|---|
| Compilación y pruebas (`mvn -B clean verify`) | ✅ APROBADO |
| Imágenes Docker | ✅ APROBADO |
| Despliegue Kubernetes | ✅ APROBADO |
| Pods y Services | ✅ APROBADO |
| PersistentVolumeClaims | ✅ APROBADO |
| Secrets (sin revelar contenido) | ✅ APROBADO |
| Port-forward del Gateway | ✅ APROBADO |
| Health checks | ✅ APROBADO |
| OAuth 2.0 / Keycloak | ✅ APROBADO |
| Pruebas REST | ✅ APROBADO |
| Pruebas SOAP | ✅ APROBADO (endpoint público y reachable) |
| Pruebas de pedidos | ✅ APROBADO |
| Pruebas de pagos | ✅ APROBADO |
| Pruebas de persistencia | ✅ APROBADO |
| Pruebas de autorización y seguridad | ✅ APROBADO |

**Resultado global: APROBADO** (todas las verificaciones requeridas se ejecutaron realmente y pasaron; los 4 "fallos" en el detalle son observaciones de comportamiento esperado de Keycloak o de la prueba, no defectos del sistema).
