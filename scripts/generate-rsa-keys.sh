#!/usr/bin/env sh
# =============================================================================
# scripts/generate-rsa-keys.sh  (adaptado para Proyecto 03 - Keycloak)
#
# Con Keycloak como unico emisor JWT, los servicios ya no necesitan el par de
# llaves RSA del Taller 02. Este script se adapta para GENERAR TODOS LOS
# SECRETOS/PASES mediante OpenSSL y escribirlos en .k8s-env (ignorado por Git).
#
# Las claves del archivo coinciden con los secretKeyRef de los manifiestos de
# k8s/ (minusculas con guiones, requerido por Kubernetes). El archivo .k8s-env
# se usa luego por scripts/k8s-setup.sh para crear el Kubernetes Secret
# tienda-secrets. Nada de lo generado se imprime por terminal ni se versiona.
# =============================================================================
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.k8s-env"

SECRET_LEN=32

gen_secret() {
    openssl rand -base64 "$SECRET_LEN" | tr -d '\n'
}

{
    echo "# Generado por scripts/generate-rsa-keys.sh - NO VERSIONAR NI COMPARTIR"
    echo "# Fecha: $(date -u +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date)"
    echo ""
    echo "# Base de datos de Keycloak"
    echo "keycloak-db-username=postgres"
    echo "keycloak-db-password=$(gen_secret)"
    echo ""
    echo "# Admin de Keycloak (usado por auth-service para crear usuarios)"
    echo "keycloak-admin-password=$(gen_secret)"
    echo ""
    echo "# Bases de datos de los microservicios (usuarios fijos 'postgres')"
    echo "user-db-username=postgres"
    echo "user-db-password=$(gen_secret)"
    echo "productos-db-username=postgres"
    echo "productos-db-password=$(gen_secret)"
    echo "pedidos-db-username=postgres"
    echo "pedidos-db-password=$(gen_secret)"
    echo "pagos-db-username=postgres"
    echo "pagos-db-password=$(gen_secret)"
    echo ""
    echo "# Secreto HMAC para validar webhooks de pago (definicion en docs/SECURITY.md)"
    echo "payment-webhook-secret=$(gen_secret)"
} > "$ENV_FILE"

chmod 600 "$ENV_FILE" 2>/dev/null || true

echo "Secretos generados en $ENV_FILE (permisos 600, ignorado por Git)."
echo "No se muestra el contenido de ningun secreto por seguridad."
echo "Siguiente paso: sh scripts/k8s-setup.sh"
