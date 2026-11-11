#!/usr/bin/env sh
# =============================================================================
# scripts/build-images.sh  (Proyecto 03 - Tienda Deportiva v3.0)
#
# Construye las imagenes Docker de los 6 microservicios y las etiqueta como
# tienda-deportiva/<servicio>:3.0.0 para consumo local por Kubernetes.
#
# NO publica las imagenes (eso requiere autorizacion expresa).
# =============================================================================
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
REGISTRY="tienda-deportiva"
VERSION="3.0.0"

SERVICES="api-gateway auth-service user-service producto-service pedido-service pago-service"

cd "$ROOT_DIR"

for svc in $SERVICES; do
    IMAGE="$REGISTRY/$svc:$VERSION"
    echo "==> Construyendo $IMAGE (Dockerfile: $svc/Dockerfile)"
    docker build \
        --build-arg VERSION="$VERSION" \
        -t "$IMAGE" \
        -f "$svc/Dockerfile" \
        "$ROOT_DIR" > /tmp/build-$svc.log 2>&1 || {
            echo "ERROR construyendo $svc. Ver /tmp/build-$svc.log"
            cat /tmp/build-$svc.log
            exit 1
        }
    echo "OK   $IMAGE"
done

echo ""
echo "Todas las imagenes se construyeron localmente:"
docker images --format "{{.Repository}}:{{.Tag}}" | grep "^tienda-deportiva/" | sort
