# Seguridad - Proyecto 03 (Tienda Deportiva v3.0)

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**

## 1. Modelo de identidad

Keycloak es el **único emisor de identidad** (OAuth 2.0 / OpenID Connect) del sistema. Emite y valida todos los JWT. auth-service **no emite JWTs propios**; solo delega registro y login en Keycloak. Esto elimina el riesgo de dos emisores JWT contradictorios (regla de arquitectura #5).

### Realm
- Nombre: `tienda-deportiva`.
- Proveedor de identidad único para todos los servicios de negocio.

## 2. Clientes OAuth 2.0 / OIDC

| Cliente | Tipo | Concesiones | Uso |
|---|---|---|---|
| `tienda-auth` | Público | password (direct access grant) | auth-service: registro y login de usuarios |
| `tienda-admin` | Confidencial | client_credentials | operaciones administrativas |

## 3. Roles

| Rol | Descripción | Uso |
|---|---|---|
| `USER` | Usuario registrado | Perfiles, productos (leer), pedidos, pagos |
| `ADMIN` | Administrador | Crear productos, actualizar estado de pagos, listar perfiles |

Los roles se publican en el claim `realm_access.roles` del JWT y se mapean a autoridades Spring `ROLE_*` (ej. `USER` → `ROLE_USER`).

## 4. Emisión y validación de tokens

- **Emisión:** Keycloak expide `access_token` mediante concesión de contraseña OIDC (`POST /realms/tienda-deportiva/protocol/openid-connect/token`).
- **Validación en servicios de negocio:** cada servicio valida el JWT mediante descubrimiento OIDC:
  - Recupera las claves públicas (JWKS) de Keycloak.
  - Valida firma RS256, expiración e issuer.
- **auth-service:** valida credenciales contra Keycloak y devuelve el token al cliente; no valida JWTs propios.

## 5. Autenticación y autorización por endpoints

### auth-service (8081)
| Endpoint | Método | Acceso |
|---|---|---|
| `/api/auth/register` | POST | Público |
| `/api/auth/login` | POST | Público |

### user-service (8082)
| Endpoint | Método | Rol |
|---|---|---|
| `/api/users` | POST | AUTENTICADO |
| `/api/users/me` | GET | AUTENTICADO (propietario) |
| `/api/users` | GET | `ADMIN` |
| `GET|PUT /api/users/{id}` | GET/PUT | propietario o `ADMIN` |
| `DELETE /api/users/{id}` | DELETE | `ADMIN` |

### producto-service (8083)
| Endpoint | Método | Rol |
|---|---|---|
| `GET /api/productos`, `GET /api/productos/{id}` | GET | AUTENTICADO |
| `POST /api/productos` | POST | `ADMIN` |
| `/ws/**` (SOAP) | POST/GET | Público (decisión documentada abajo) |

### pedido-service (8084)
| Endpoint | Método | Rol |
|---|---|---|
| `GET /api/pedidos`, `POST /api/pedidos` | GET/POST | AUTENTICADO (`USER`, `ADMIN`) |

### pago-service (8085)
| Endpoint | Método | Rol |
|---|---|---|
| `POST /api/pagos` | POST | `USER`, `ADMIN` |
| `GET /api/pagos/{id}`, `GET /api/pagos/pedido/{pedidoId}` | GET | `USER`, `ADMIN` |
| `POST /api/pagos/{id}/estado` | POST | `ADMIN` |

## 6. Integración del auth-service existente

auth-service se integra con Keycloak:
- **Registro:** crea el usuario en Keycloak mediante su API de administración (incluye `firstName`/`lastName` para que la cuenta quede completamente configurada y pueda autenticarse por password grant).
- **Login:** obtiene el `access_token` de Keycloak mediante concesión de contraseña OIDC y lo devuelve al cliente.

## 7. Gestión segura de contraseñas, secretos y llaves

| Elemento | Gestión |
|---|---|
| Contraseñas de usuario | Keycloak (never stored by auth-service) |
| Contraseñas de servicios y admin de Keycloak | OpenSSL (`openssl rand -base64 32`), inyectadas vía Kubernetes Secret `tienda-secrets` |
| Llaves JWT | Keycloak gestiona sus propias llaves internas; **no se distribuyen llaves RSA públicas/privadas entre servicios** |
| `.k8s-env` | Ignorado por Git (`.gitignore`), permisos 600, nunca se muestra en terminal |
| Kubernetes Secret `tienda-secrets` | Creado desde `.k8s-env`; solo metadatos visibles, nunca el valor |

## 8. Secreto HMAC para webhook de pago

El documento de requisitos menciona un "secreto HMAC para webhook". El PDF **no especifica** su propósito, contrato ni una integración externa real. Por decisión de diseño (D-04):

- **Propósito:** validar la autenticidad de webhooks internos de notificación de estado de pago (ej. que una notificación "pago aprobado" provenga de origen de confianza).
- **Contrato:** el secreto se denomina `PAYMENT_WEBHOOK_SECRET`, se genera con OpenSSL y se inyecta en `pago-service` mediante la propiedad `pago.webhook.secret` (desde el Secret `tienda-secrets`).
- **Integración externa:** **no existe** ninguna integración con un proveedor de pagos externo real. Es un contrato definido y documentado por el equipo, no una integración externa.

## 9. SOAP `/ws` (decisión documentada)

El servicio SOAP `/ws/**` se mantiene **público** para preservar la interoperabilidad con clientes SOAP clásicos del proyecto original. Esta es una decisión consciente del entorno académico (riesgo aceptado), documentada en el Taller 02 y mantenida en Proyecto 03.

## 10. Contraseñas de usuario de prueba (entorno académico)

| Rol | Usuario | Contraseña |
|---|---|---|
| ADMIN | `admin1` | `Admin123!` |
| USER | `valeria` | `Valeria123!` |
| USER | `student` | `Student123!` |

Solo para el taller. Nunca en logs ni respuestas.

## 11. Políticas de seguridad

- Todo el tráfico entre servicios usa HTTPS interno de Kubernetes (ClusterIP) + DNS interno.
- Los secretos nunca viajan en texto plano en comandos ni logs.
- Los manifiestos de Kubernetes no contienen credenciales reales (usan `secretKeyRef`).
- El SOAP se mantiene público (riesgo aceptado, entorno académico).
