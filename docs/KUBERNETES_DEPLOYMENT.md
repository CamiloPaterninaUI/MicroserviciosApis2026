# Despliegue Kubernetes - Proyecto 03

**Universidad de Ibagué — Web, APIs y Microservicios — Grupo 06**

## 1. Visión general

Todos los manifiestos residen en el directorio **`k8s/`** y se despliegan en el namespace **`proyecto-03`**. Se despliegan 12 recursos de tipo Deployment (6 aplicaciones + 6 bases de datos/Keycloak), 12 Servicios (ClusterIP), 6 PersistentVolumeClaim, 1 ConfigMap, 1 Secret y 1 Job.

## 2. Estructura de directorios

```
k8s/
├── namespace.yaml                  # Namespace proyecto-03
├── keycloak.yaml                   # Keycloak + Service
├── keycloak-postgres.yaml          # PostgreSQL de Keycloak + PVC + Service
├── keycloak-realm-config.yaml      # Job: crea realm, roles y clientes
├── keycloak-realm-config.yaml      # (configuración del realm)
├── auth.yaml                       # auth-service (sin BD propia)
├── gateway.yaml                    # api-gateway (punto de entrada)
├── user.yaml                       # user-service + user-postgres + PVC + Service
├── producto.yaml                   # producto-service + productos-postgres + PVC + Service
├── pedido.yaml                     # pedido-service + pedidos-postgres + PVC + Service
├── pago.yaml                       # pago-service + pagos-postgres + PVC + Service
└── (todos los manifiestos se aplican con: kubectl apply -f k8s/ -n proyecto-03)
```

## 3. Namespace

`k8s/namespace.yaml` crea el namespace `proyecto-03`. Todos los recursos se aplican con `-n proyecto-03`.

## 4. Despliegues de microservicios

Cada aplicación tiene su propio Deployment y Service (`ClusterIP`). Los puertos contenedor son:

| Servicio | Puerto contenedor | Puerto Service |
|---|---|---|
| api-gateway | 8080 | 8080 |
| auth-service | 8081 | 8081 |
| user-service | 8082 | 8082 |
| producto-service | 8083 | 8083 |
| pedido-service | 8084 | 8084 |
| pago-service | 8085 | 8085 |

### Puerto del pedido-service
El pedido-service está en el puerto **8084** para evitar conflictos con auth-service (8081) y user-service (8082), que es el contrato del Proyecto 03 (regla de arquitectura #8).

## 5. Comunicación interna (DNS de Kubernetes)

**Toda la comunicación entre Pods usa DNS interno de Kubernetes, nunca `localhost`:**

- `http://keycloak:8080` (auth-service → Keycloak; apps → JWKS de Keycloak)
- `http://auth-service:8081`, `http://user-service:8082`, `http://producto-service:8083`, `http://pedido-service:8084`, `http://pago-service:8085` (gateway → servicios)
- `http://producto-service:8083/api/productos/` (pedido-service consulta precios)
- `jdbc:postgresql://<nombre-servicio-db>:5432/<bd>` (cada app → su PostgreSQL)

Ejemplo de variable de entorno en pedido-service:
```
PRODUCTO_SERVICE_URL: http://producto-service:8083/api/productos/
```

## 6. Bases de datos PostgreSQL independientes

Cada servicio de negocio tiene su propia PostgreSQL (Database per Service):

| Servicio | Base de datos | PVC | Service |
|---|---|---|---|
| user-service | users_db | user-postgres-pvc | user-db |
| producto-service | productos_db | productos-postgres-pvc | productos-db |
| pedido-service | pedidos_db | pedidos-postgres-pvc | pedidos-db |
| pago-service | pagos_db | pagos-postgres-pvc | pagos-db |
| Keycloak | keycloak | keycloak-postgres-pvc | keycloak-postgres |

auth-service **no tiene PostgreSQL propia** (delega la identidad en Keycloak).

Cada PostgreSQL usa:
- Imagen `postgres:17-alpine`.
- `storageClassName: hostpath` (el clúster no tiene `local-path`; los disponibles son `hostpath` y `standard`).
- Credenciales desde el Secret `tienda-secrets` (claves `*-db-username`, `*-db-password`).
- `POSTGRES_USER=postgres` (usuario fijo).

## 7. PersistentVolumeClaims

Cada PVC pide **2Gi** con `ReadWriteOnce` y `storageClassName: hostpath`. Ejemplo:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: user-postgres-pvc
  namespace: proyecto-03
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: hostpath
  resources:
    requests:
      storage: 2Gi
```

> **Importante:** no eliminar PVC con `-v` sin autorización, ya que borraría los datos. Los volúmenes persisten tras reiniciar contenedores (sin `-v`).

## 8. ConfigMaps

### `keycloak-realm`
Contiene el archivo `01-realm-tienda.json` con la definición del realm `tienda-deportiva` (usuarios, roles, clientes). Se monta como volumen de solo lectura en el Job de configuración del realm.

## 9. Kubernetes Secrets

### `tienda-secrets`
Creado desde `.k8s-env` (generado por `scripts/generate-rsa-keys.sh` con OpenSSL). Contiene:

| Clave | Descripción |
|---|---|
| `keycloak-db-username` / `keycloak-db-password` | BD de Keycloak |
| `keycloak-admin-password` | Admin de Keycloak (usado por auth-service y Job de realm) |
| `user-db-username` / `user-db-password` | BD de user-service |
| `productos-db-username` / `productos-db-password` | BD de producto-service |
| `pedidos-db-username` / `pedidos-db-password` | BD de pedido-service |
| `pagos-db-username` / `pagos-db-password` | BD de pago-service |
| `payment-webhook-secret` | Secreto HMAC para webhooks de pago (contrato propio, ver docs/SECURITY.md) |

**Nunca se revela el contenido de los secretos.** Para verificarlos sin revelar su contenido:
```bash
kubectl get secret tienda-secrets -n proyecto-03   # solo metadatos
kubectl get secret tienda-secrets -n proyecto-03 -o yaml | head -5  # solo encabezado
```

## 10. Readiness y Liveness probes

| Componente | Readiness | Liveness |
|---|---|---|
| Apps (6) | `httpGet /actuator:80XX/health` | `httpGet /actuator:80XX/health` |
| PostgreSQL (6) | `exec pg_isready` | `exec pg_isready` |
| Keycloak | `tcpSocket :8080` | `tcpSocket :8080` |

Todos con `initialDelaySeconds`, `periodSeconds`, `timeoutSeconds` y `failureThreshold` razonables para Docker Desktop.

## 11. Recursos CPU/memoria

| Componente | CPU request | CPU limit | Memoria request | Memoria limit |
|---|---|---|---|---|
| Apps (6) | 250m | 500m | 256Mi | 512Mi |
| Keycloak | 500m | 1000m | 512Mi | 1Gi |
| PostgreSQL (7) | 250m | 500m | 256Mi | 512Mi |

Valores razonables para Docker Desktop.

## 12. Políticas de reinicio y persistencia

- `restartPolicy: Always` en todos los Deployments.
- Los datos persisten en los PVC tras reiniciar contenedores (sin `-v`).
- **Peligroso:** `kubectl delete pvc <nombre> -n proyecto-03` o `kubectl delete pvc <nombre> -n proyecto-03` elimina el volumen y sus datos.

## 13. Scripts de automatización

| Script | Propósito |
|---|---|
| `scripts/generate-rsa-keys.sh` | Genera TODOS los secretos con OpenSSL y los escribe en `.k8s-env` (ignorado por Git, permisos 600). |
| `scripts/build-images.sh` | Construye las 6 imágenes `tienda-deportiva/<servicio>:3.0.0` localmente. |
| `scripts/k8s-setup.sh` | Aplica manifiestos, crea el Secret `tienda-secrets` desde `.k8s-env` y espera los rollouts. |

## 14. Despliegue reproducible

```bash
# Desde una clonación nueva
sh scripts/generate-rsa-keys.sh     # genera secretos (OpenSSL)
sh scripts/build-images.sh          # construye imágenes Docker
sh scripts/k8s-setup.sh             # despliega en Kubernetes y espera rollouts
```

## 15. Verificación

```bash
kubectl get pods -n proyecto-03              # todos los pods Running
kubectl get deployments -n proyecto-03       # READY 1/1, AVAILABLE 1
kubectl get services -n proyecto-03          # servicios ClusterIP
kubectl get pvc -n proyecto-03               # PVC Bound
kubectl get secret tienda-secrets -n proyecto-03  # solo metadatos (no revelar valores)
kubectl port-forward svc/api-gateway 8080:8080 -n proyecto-03
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

## 16. Habilitar Kubernetes (si no está activo)

Si Kubernetes no está habilitado en Docker Desktop:
1. Abre **Docker Desktop**.
2. Ve a **Settings → Kubernetes**.
3. Marca **Enable Kubernetes** y aplica.
4. Espera a que aparezca el mensaje **"Kubernetes is running"**.
5. Verifica con `kubectl cluster-info` y `kubectl get nodes` (debe mostrar `Ready`).

Si Kubernetes no está habilitado, el despliegue queda **bloqueado** hasta habilitarlo; no hay alternativa en el alcance de este proyecto (los manifiestos están listos para aplicar una vez habilitado).
