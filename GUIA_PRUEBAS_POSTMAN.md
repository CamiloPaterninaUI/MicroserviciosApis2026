# 📮 Guía de pruebas manuales en Postman — Tienda Deportiva v2.0

Guía práctica para probar **todos los endpoints** de los microservicios desde Postman con el proyecto corriendo. Reemplaza cualquier `{token}` por el `accessToken` real que obtengas en el paso de login.

## 0. Prerrequisitos

1. Levanta el proyecto (ver `README.md`):

   ```bash
   cp .env.example .env
   sh scripts/generate-rsa-keys.sh
   docker compose up --build -d
   ```

2. Confirma que todo está sano: `docker compose ps` (9 servicios Up) y `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

## 1. URLs base

| Forma | URL base | Cuándo usarla |
|---|---|---|
| **Por el Gateway** (recomendado) | `http://localhost:8080` | Así corre la arquitectura real; toda respuesta incluye el header `X-Correlation-Id` |
| **Directo al servicio** | `http://localhost:808X` | Para probar cada microservicio aislado o su Swagger |

URLs directas: auth `:8081` · users `:8082` · productos `:8083` · pedidos `:8084` · Wsdl `:8083/ws/productos.wsdl`

**Usuarios demo cargados al arrancar** (solo entorno académico):

| Rol | Usuario | Contraseña |
|---|---|---|
| ADMIN | `admin` | `Admin123!` |
| USER | `student` | `Student123!` |

## 2. Orden sugerido

**Registro → Login (copia el token) → Perfil → Catálogo → Pedido → SOAP → casos negativos.**

> 💡 En Postman puedes guardar el token una vez: pestaña **Authorization → Type: Bearer Token** (pega solo el JWT) o el header manual `Authorization: Bearer <token>`. También puedes importar la colección automatizada de `postman/` que guarda los tokens sola.

---

## 3. AUTH SERVICE (register / login)

### 3.1 Registrar usuario nuevo → esperado **201**

- **POST** `http://localhost:8080/api/auth/register`
- Header: `Content-Type: application/json`
- Body → **raw → JSON**:

```json
{
  "username": "maria.prueba",
  "email": "maria.prueba@example.com",
  "password": "Clave123"
}
```

- Respuesta esperada:

```json
{
  "userId": "3f0a...",
  "username": "maria.prueba",
  "email": "maria.prueba@example.com",
  "role": "USER"
}
```

### 3.2 Login USER → esperado **200**

- **POST** `http://localhost:8080/api/auth/login`

```json
{ "username": "student", "password": "Student123!" }
```

- Respuesta esperada (copia `accessToken`):

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJraWQiOiJ0YWxsZXItMi1rZXkiLCJhbGciOiJSUzI1NiJ9...",
  "expiresAt": "2026-10-13T19:20:30Z",
  "userId": "9a57...",
  "username": "student",
  "role": "USER"
}
```

### 3.3 Login ADMIN → misma URL con:

```json
{ "username": "admin", "password": "Admin123!" }
```

### 3.4 Casos negativos

| Caso | Body | Esperado |
|---|---|---|
| Registro duplicado | el mismo de 3.1 | **409** "El nombre de usuario ya está registrado" |
| Datos inválidos | `{"username":"ab","email":"no-es-correo","password":"corta1"}` | **400** con `validationErrors` por campo |
| Credenciales incorrectas | login con `"password": "Incorrecta1"` | **401** "Credenciales inválidas" |

---

## 4. USER SERVICE — perfiles (requiere token)

| # | Método | URL | Body | Esperado |
|---|---|---|---|---|
| 1 | POST | `http://localhost:8080/api/users` | (abajo) | **201** |
| 2 | GET | `http://localhost:8080/api/users/me` | — | **200** tu perfil |
| 3 | GET | `http://localhost:8080/api/users` | — | **200** con ADMIN · **403** con USER |
| 4 | GET | `http://localhost:8080/api/users/{tu userId}` | — | **200** (propietario o ADMIN) |
| 5 | PUT | `http://localhost:8080/api/users/{tu userId}` | (abajo) | **200** |
| 6 | DELETE | `http://localhost:8080/api/users/{id}` con ADMIN | — | **204** |
| 7 | GET | `http://localhost:8080/api/users/me` **sin token** | — | **401** |

**Crear perfil (POST):**

```json
{
  "firstName": "Estudiante",
  "lastName": "Demo",
  "phone": "+57 300 111 2233"
}
```

**Actualizar perfil (PUT):**

```json
{
  "firstName": "Estudiante",
  "lastName": "Demo Actualizado",
  "phone": "3001234567"
}
```

> ⚠️ Con el token de `student` el POST de perfil responde **409** porque su perfil ya existe de las pruebas previas — ese es el contrato correcto (duplicado → 409). Usa el token de tu usuario recién registrado para un 201 limpio.

---

## 5. PRODUCTO SERVICE — catálogo (requiere token)

| # | Método | URL | Body | Esperado |
|---|---|---|---|---|
| 1 | GET | `http://localhost:8080/api/productos` | — | **200** lista con las 3 semillas |
| 2 | GET | `http://localhost:8080/api/productos/1` | — | **200** "Camiseta Selección Colombia", `precio: 120000.00` |
| 3 | GET | `http://localhost:8080/api/productos/9999` | — | **404** |
| 4 | GET | `http://localhost:8080/api/productos` **sin token** | — | **401** |
| 5 | POST | `http://localhost:8080/api/productos` con **ADMIN** | (abajo) | **201** |
| 6 | POST | con **USER** (student) | (abajo) | **403** |
| 7 | POST | con ADMIN pero body inválido | (abajo) | **400** por campo |

**Crear producto (solo ADMIN):**

```json
{
  "nombre": "Bicicleta de montaña",
  "precio": 850000.99,
  "stock": 8
}
```

**Body inválido (400):**

```json
{ "nombre": "", "precio": -10, "stock": -1 }
```

> Productos semilla disponibles: **1** Camiseta (120000.00), **2** Balón (85000.00), **3** Guayos (250000.00).

---

## 6. PEDIDO SERVICE — pedidos (requiere token · usa **query params**, no body)

| # | Método | URL | Esperado |
|---|---|---|---|
| 1 | POST | `http://localhost:8080/api/pedidos?productoId=2&cantidad=3` | **200** con `total: 255000.00` |
| 2 | GET | `http://localhost:8080/api/pedidos` | **200** lista de pedidos |
| 3 | POST | `...?productoId=9999&cantidad=1` | **404** "Producto no encontrado en producto-service…" |
| 4 | POST | `...?productoId=2&cantidad=0` | **400** |
| 5 | GET | `http://localhost:8080/api/pedidos` **sin token** | **401** |

- En Postman: pestaña **Params** → Key `productoId` = `2`, Key `cantidad` = `3` (o pega la URL completa).
- Respuesta esperada del POST:

```json
{
  "id": 4,
  "productoId": 2,
  "cantidad": 3,
  "total": 255000.00
}
```

> El total lo calcula **pedido-service** consultando el precio a **producto-service** con tu propio JWT propagado, usando BigDecimal exacto.

---

## 7. SOAP (sin token; también enrutado por el Gateway)

### 7.1 WSDL

- **GET** `http://localhost:8080/ws/productos.wsdl` (navegador o Postman) → XML del contrato original: namespace `http://unibague.edu.co/productos`, port `ProductoPort`.

### 7.2 Operación getProductoRequest

- **POST** `http://localhost:8080/ws`
- Headers: `Content-Type: text/xml; charset=utf-8`
- Body → **raw → XML**:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:prod="http://unibague.edu.co/productos">
  <soapenv:Header/>
  <soapenv:Body>
    <prod:getProductoRequest>
      <prod:id>1</prod:id>
    </prod:getProductoRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

- → **200** con `<getProductoResponse>` y la camiseta.
- Cambia `<prod:id>` a `9999` → **SOAP Fault** (producto inexistente).

---

## 8. Extras

**Health de cada microservicio (GET, sin token):**

```
http://localhost:8080/actuator/health   (gateway)
http://localhost:8081/actuator/health   (auth)
http://localhost:8082/actuator/health   (users)
http://localhost:8083/actuator/health   (productos)
http://localhost:8084/actuator/health   (pedidos)
```

**Swagger (interactivo, con botón Authorize para pegar el JWT):**

```
http://localhost:8081/swagger-ui.html
http://localhost:8082/swagger-ui.html
http://localhost:8083/swagger-ui.html
http://localhost:8084/swagger-ui.html
```

**Verifica que pasas por el Gateway:** mira el header de respuesta `X-Correlation-Id` (un UUID distinto por request; si lo envías tú con tu propio valor, se devuelve igual).

## 9. Colección automatizada (opcional)

Si prefieres la versión automática (guarda tokens e IDs sola y corre todo en orden):

```bash
newman run postman/Tienda_Deportiva_v2.postman_collection.json \
  -e postman/Tienda_Deportiva_v2.postman_environment.json
```

O importa ambos archivos de `postman/` en la app de Postman y ejecuta la colección con Runner.

## 10. Códigos de la guía rápida

| Código | Significado en el taller |
|---|---|
| 200/201/204 | Operación correcta (leer/crear/eliminar) |
| 400 | Datos inválidos (validación por campo) |
| 401 | Sin token, token inválido/expirado o credenciales incorrectas |
| 403 | Autenticado pero sin permiso (p. ej. USER creando productos) |
| 404 | Recurso inexistente (producto, perfil, pedido) |
| 409 | Duplicado (username/email/perfil) |
| 502 | producto-service no disponible al crear un pedido |
