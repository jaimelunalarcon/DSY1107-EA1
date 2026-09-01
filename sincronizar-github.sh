#!/usr/bin/env bash
# Lee outputs de Terraform y configura variables de GitHub Actions (requiere gh CLI).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
TF_DIR="$ROOT/terraform"

if ! command -v gh >/dev/null 2>&1; then
  echo "Instala GitHub CLI: https://cli.github.com/" >&2
  exit 1
fi

cd "$TF_DIR"

REGION="$(terraform output -raw aws_region)"
POOL_ID="$(terraform output -raw cognito_user_pool_id)"
CLIENT_ID="$(terraform output -raw cognito_client_id)"
COGNITO_DOMAIN="$(terraform output -raw cognito_domain)"
API_URL="$(terraform output -raw api_base_url)"
APP_ID="$(terraform output -raw amplify_app_id)"
AMPLIFY_URL="$(terraform output -raw amplify_url)"

echo "Configurando variables del repositorio..."
gh variable set AWS_REGION --body "$REGION"
gh variable set COGNITO_USER_POOL_ID --body "$POOL_ID"
gh variable set COGNITO_CLIENT_ID --body "$CLIENT_ID"
gh variable set COGNITO_DOMAIN --body "$COGNITO_DOMAIN"
gh variable set API_URL --body "$API_URL"
gh variable set AMPLIFY_APP_ID --body "$APP_ID"
gh variable set REDIRECT_URI --body "${AMPLIFY_URL}/"
gh variable set AMPLIFY_BRANCH --body "main"

echo "Listo. Revisa secrets AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY y AWS_SESSION_TOKEN en GitHub."
