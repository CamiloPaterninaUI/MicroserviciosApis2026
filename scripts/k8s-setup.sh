#!/usr/bin/env sh
# =============================================================================
# scripts/k8s-setup.sh  (Proyecto 03 - Kubernetes + Keycloak)
#
# Despliega toda la tienda en el namespace proyecto-03:
#   1. Crea el namespace.
#   2. Aplica todos los manifiestos de k8s/.
#   3. Crea el Secret tienda-secrets desde .k8s-env (generado por
#      scripts/generate-rsa-keys.sh).
#   4. Espera a que todos los Deployments estén listos.
#   5. Muestra el estado de Pods, Servicios y PVCs (sin revelar secretos).
#
# Requisitos: kubectl configurado contra un clúster (Docker Desktop Kubernetes)
# y .k8s-env generado previamente con scripts/generate-rsa-keys.sh.
# =============================================================================
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.k8s-env"
NAMESPACE="proyecto-03"

command -v kubectl >/dev/null 2>&1 || {
    echo "ERROR: kubectl no esta en el PATH. Instalelo o habilite Docker Desktop Kubernetes."
    exit 1
}

echo "==> Verificando clúster Kubernetes"
kubectl cluster-info >/dev/null 2>&1 || {
    echo "ERROR: no hay un clúster Kubernetes alcanzable. Habilite Kubernetes en Docker Desktop."
    exit 1
}
echo "OK clúster alcanzable"

echo "==> Creando namespace $NAMESPACE (si no existe)"
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f - >/dev/null

echo "==> Aplicando manifiestos de k8s/"
kubectl apply -f "$ROOT_DIR/k8s/" -n "$NAMESPACE"

if [ ! -f "$ENV_FILE" ]; then
    echo "ADVERTENCIA: no se encontro $ENV_FILE. Ejecute primero scripts/generate-rsa-keys.sh."
    echo "El Secret tienda-secrets no se creara y los pods con secretKeyRef no arrancaran."
else
    echo "==> Creando Secret tienda-secrets (desde $ENV_FILE, sin mostrar contenido)"
    kubectl create secret generic tienda-secrets \
        --from-env-file="$ENV_FILE" \
        --dry-run=client -o yaml | kubectl apply -f - -n "$NAMESPACE" >/dev/null
    echo "OK Secret tienda-secrets aplicado (claves: $(grep -v '^#' "$ENV_FILE" | grep -v '^$' | cut -d= -f1 | tr '\n' ' '))"
fi

echo "==> Esperando rollouts de los Deployments (puede tardar varios minutos)"
kubectl rollout status deployment/keycloak-postgres -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/keycloak -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/user-postgres -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/user-service -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/productos-postgres -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/producto-service -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/pedidos-postgres -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/pedido-service -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/pagos-postgres -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/pago-service -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/auth-service -n "$NAMESPACE" --timeout=300s >/dev/null
kubectl rollout status deployment/api-gateway -n "$NAMESPACE" --timeout=300s >/dev/null

echo ""
echo "==> Estado del despliegue en namespace $NAMESPACE"
kubectl get pods -n "$NAMESPACE"
echo ""
kubectl get services -n "$NAMESPACE"
echo ""
kubectl get pvc -n "$NAMESPACE"
echo ""
echo "OK Despliegue completado. El Gateway se expone con:"
echo "   kubectl port-forward svc/api-gateway 8080:8080 -n $NAMESPACE"
