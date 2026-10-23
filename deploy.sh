#!/usr/bin/env bash
# =============================================================================
# 1.2.9e — Del localhost al dominio público (React + Amplify)
#
#   ./deploy.sh              genera config Amplify, build y publica
#   ./deploy.sh --config-local   escribe config.json para npm run dev (:5173)
#   ./deploy.sh --config-local --api-local
#                            igual, pero el front llama al backend local
#                            (API_LOCAL_URL, por defecto http://localhost:8080)
#                            en vez del API Gateway: demo RabbitMQ de la EP2
# =============================================================================

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TF_DIR="$RAIZ/terraform"
FRONTEND="$RAIZ/frontend"
MODO="amplify"
API_LOCAL=0
USO="Uso: ./deploy.sh [--config-local [--api-local]]"

case "$*" in
  "") ;;
  "--config-local") MODO="local" ;;
  "--config-local --api-local" | "--api-local --config-local") MODO="local"; API_LOCAL=1 ;;
  *) echo "$USO" >&2; exit 1 ;;
esac

for h in terraform python3; do
  command -v "$h" >/dev/null 2>&1 || { echo "Falta '$h' en el PATH." >&2; exit 1; }
done

cd "$TF_DIR"

salida() {
  terraform output -raw "$1" 2>/dev/null || {
    echo "Falta el output '$1' de Terraform. Ejecuta primero:" >&2
    echo "  cd terraform && terraform apply" >&2
    exit 1
  }
}

REGION="$(salida aws_region)"
CLIENT_ID="$(salida cognito_client_id)"
COGNITO_DOMAIN="$(salida cognito_domain)"

if [[ "$API_LOCAL" == "1" ]]; then
  # El backend local no valida el JWT (lo hace el API Gateway): el login sigue
  # siendo Cognito y los scopes del token deciden que muestra el front.
  API_URL="${API_LOCAL_URL:-http://localhost:8080}"
else
  API_URL="$(salida api_base_url)"
fi

export REGION CLIENT_ID COGNITO_DOMAIN API_URL

if [[ "$MODO" == "local" ]]; then
  export REDIRECT_URI="http://localhost:5173/"
  export EXIGIR_HTTPS=0
  "$RAIZ/scripts/config-frontend.sh"
  echo
  echo "API: $API_URL"
  echo "Config local lista. Arranca el front:"
  echo "  cd frontend && npm run dev"
  echo "  → http://localhost:5173/"
  exit 0
fi

APP_ID="$(salida amplify_app_id)"
AMPLIFY_URL="$(salida amplify_url)"

export REDIRECT_URI="${AMPLIFY_URL}/"
export EXIGIR_HTTPS=1
"$RAIZ/scripts/config-frontend.sh"

cd "$FRONTEND"
if [[ -f package-lock.json ]]; then
  npm ci
else
  npm install
fi
npm run build

"$RAIZ/scripts/publicar-amplify.sh" "$APP_ID" main "$FRONTEND/dist"

echo
echo "Front publicado en: $AMPLIFY_URL"
echo "Para volver a local: ./deploy.sh --config-local"
