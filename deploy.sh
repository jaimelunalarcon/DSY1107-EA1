#!/usr/bin/env bash
# =============================================================================
# 1.2.9e — Del localhost al dominio público (React + Amplify)
#
#   ./deploy.sh              genera config Amplify, build y publica
#   ./deploy.sh --config-local   escribe config.json para npm run dev (:5173)
# =============================================================================

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TF_DIR="$RAIZ/terraform"
FRONTEND="$RAIZ/frontend"
MODO="amplify"

if [[ "${1:-}" == "--config-local" ]]; then
  MODO="local"
elif [[ -n "${1:-}" ]]; then
  echo "Uso: ./deploy.sh [--config-local]" >&2
  exit 1
fi

for h in terraform python3; do
  command -v "$h" >/dev/null 2>&1 || { echo "Falta '$h' en el PATH." >&2; exit 1; }
done

cd "$TF_DIR"

if ! terraform output -raw aws_region >/dev/null 2>&1; then
  echo "No hay outputs de Terraform. Ejecuta primero:" >&2
  echo "  cd terraform && terraform apply" >&2
  exit 1
fi

REGION="$(terraform output -raw aws_region)"
CLIENT_ID="$(terraform output -raw cognito_client_id)"
COGNITO_DOMAIN="$(terraform output -raw cognito_domain)"
API_URL="$(terraform output -raw api_base_url)"
APP_ID="$(terraform output -raw amplify_app_id)"
AMPLIFY_URL="$(terraform output -raw amplify_url)"

export REGION CLIENT_ID COGNITO_DOMAIN API_URL

if [[ "$MODO" == "local" ]]; then
  export REDIRECT_URI="http://localhost:5173/"
  export EXIGIR_HTTPS=0
  "$RAIZ/scripts/config-frontend.sh"
  echo
  echo "Config local lista. Arranca el front:"
  echo "  cd frontend && npm run dev"
  echo "  → http://localhost:5173/"
  exit 0
fi

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
