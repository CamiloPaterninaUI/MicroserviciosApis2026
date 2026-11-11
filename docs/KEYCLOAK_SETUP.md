# Keycloak Setup - Proyecto 03

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**

## 1. Visión general

Keycloak es el **único emisor de identidad** (OAuth 2.0 / OpenID Connect) de la Tienda Deportiva v3.0. Emite y valida los JWT que usan todos los servicios de negocio. auth-service **no emite JWTs propios**; solo delega el registro y login de usuarios en Keycloak.

## 2. Despliegue en Kubernetes

Keycloak se despliega con:
- **Deployment `keycloak`** (imagen `quay.io/keycloak/keycloak:26.0.1`, modo `start-dev`, puerto 8080).
- **Deployment `keycloak-postgres`** (PostgreSQL 17-alpine, base de datos `keycloak`).
- **Service `keycloak`** (ClusterIP :8080) y **Service `keycloak-postgres`** (ClusterIP :5432).
- **PersistentVolumeClaim `keycloak-postgres-pvc`** (2Gi, `ReadWriteOnce`).

Todas las contraseñas se inyectan desde el Kubernetes Secret `tienda-secrets` (generado con OpenSSL por `scripts/generate-rsa-keys.sh`; **nunca en Git ni en logs**).

## 3. Realm, clientes y roles

El realm `tienda-deportiva` se crea mediante el **Job `keycloak-realm-config`** que ejecuta `kcadm.sh` contra la API de administración de Keycloak. Configura:

### Realm
| Campo | Valor |
|---|---|
| id / realm | `tienda-deportiva` |
| displayName | `Tienda Deportiva - Proyecto 03` |
| enabled | `true` |
| registrationAllowed | `true` |

### Roles de realm
| Rol | Descripción |
|---|---|
| `USER` | Usuario registrado |
| `ADMIN` | Administrador |

### Clientes OAuth 2.0 / OIDC

| Cliente | Tipo | Concesiones | Uso |
|---|---|---|---|
| `tienda-auth` | Público | password (`directAccessGrantsEnabled`) | auth-service: registro y login de usuarios |
| `tienda-admin` | Confidencial | client_credentials (service accounts) | operaciones administrativas |

El cliente `tienda-auth` tiene `redirectUris` vacío y `webOrigins: ["*"]` (entorno académico).

## 4. Emisión y validación de tokens

- **Emisión:** Keycloak expide los access_token (concesión de contraseña OIDC) mediante `POST /realms/tienda-deportiva/protocol/openid-connect/token`.
- **Validación:** Cada servicio de negocio valida los JWT mediante descubrimiento OIDC:
  - Recupera las claves públicas de Keycloak desde `/realms/tienda-deportiva/protocol/openid-connect/certs` (JWKS).
  - Valida firma (RS256), expiración y emisor (`iss = http://keycloak:8080/realms/tienda-deportiva`).
- **Roles:** Los roles se publican en el claim `realm_access.roles` de Keycloak y se mapean a autoridades Spring `ROLE_*` (por ejemplo, `USER` → `ROLE_USER`, `ADMIN` → `ROLE_ADMIN`).

## 5. Autorización por roles

| Recurso | Acción | Rol requerido |
|---|---|---|
| Perfiles (CRUD) | leer/escribir | AUTENTICADO (propietario o ADMIN) |
| Catálogo de productos | leer | AUTENTICADO |
| Crear producto | `POST /api/productos` | `ADMIN` |
| Pedidos | leer/crear | `USER`, `ADMIN` |
| Pagos | leer/crear | `USER`, `ADMIN` |
| Actualizar estado de pago | `POST /api/pagos/{id}/estado` | `ADMIN` |

## 6. Integración del auth-service existente

auth-service deja de ser emisor de JWT. Ahora:
- **`POST /api/auth/register`**: crea el usuario en Keycloak mediante la API de administración (cliente `tienda-admin`, credenciales de admin de Keycloak). Incluye `firstName` y `lastName` para que la cuenta quede "completamente configurada" en Keycloak y pueda autenticarse por password grant.
- **`POST /api/auth/login`**: obtiene el access_token de Keycloak mediante concesión de contraseña OIDC (cliente público `tienda-auth`) y se lo devuelve al cliente.

auth-service **no tiene base de datos propia**; la identidad vive exclusivamente en Keycloak.

## 7. Gestión segura de contraseñas, secretos y llaves

- **Contraseñas de usuario:** gestionadas por Keycloak (never stored by auth-service).
- **Contraseñas de servicio y admin de Keycloak:** generadas con OpenSSL (`openssl rand -base64 32`), inyectadas mediante el Kubernetes Secret `tienda-secrets`.
- **Llaves RSA JWT:** **ya no se usan** (Keycloak gestiona sus propias llaves de firma internas). El script `scripts/generate-rsa-keys.sh` se adaptó para generar secretos de despliegue, no llaves JWT.
- **Nunca en Git:** `.k8s-env` está en `.gitignore` con permisos 600.

## 8. Secreto HMAC para webhook de pago (contrato propio)

El documento de requisitos menciona un "secreto HMAC para webhook". El PDF **no especifica** su propósito ni contrato ni una integración externa real. Por decisión de diseño (D-04):

- **Propósito:** validar la autenticidad de webhooks internos de notificación de estado de pago (por ejemplo, que una notificación de "pago aprobado" provenga de una origen de confianza).
- **Contrato:** el secreto se llama `PAYMENT_WEBHOOK_SECRET`, se genera con OpenSSL y se inyecta en `pago-service` mediante el Secret `tienda-secrets`. El servicio lo expone mediante la propiedad `pago.webhook.secret`.
- **Integración externa:** **no existe** integración externa real; es un contrato definido y documentado por el equipo, no una integración con un proveedor de pagos externo.

## 9. Usuarios de prueba

Creados vía Keycloak (no se hardcodean):

| Rol | Usuario | Contraseña | Cómo se crea |
|---|---|---|---|
| ADMIN | `admin1` | `Admin123!` | `kcadm.sh` (Job de inicialización) + `set-password` |
| USER | `valeria` | `Valeria123!` | `kcadm.sh` + `set-password` + rol `USER` |
| USER | `student` | `Student123!` | `kcadm.sh` + `set-password` + rol `USER` |

> Las contraseñas son de uso académico y solo para el entorno de prueba. Nunca se revelan en logs ni reportes.

## 10. Archivos relacionados

- `k8s/keycloak.yaml` — Deployment y Service de Keycloak y su PostgreSQL.
- `k8s/keycloak-realm-config.yaml` — Job que crea el realm, roles y clientes.
- `k8s/keycloak-postgres.yaml` — PostgreSQL dedicada de Keycloak.
- `scripts/generate-rsa-keys.sh` — genera los secretos de despliegue con OpenSSL.
- `scripts/k8s-setup.sh` — crea el Secret `tienda-secrets` y despliega todo.
- `docs/SECURITY.md` — modelo de identidad, secretos y políticas de seguridad.
