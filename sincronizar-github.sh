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

if ! terraform output -raw aws_region >/dev/null 2>&1; then
  echo "No hay outputs. Ejecuta terraform apply primero." >&2
  exit 1
fi

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

# 1.3.9 — ECS (si ya se aplicó ecs.tf / rds.tf)
if terraform output -raw ecs_repositorio >/dev/null 2>&1; then
  gh variable set ECR_REPO --body "$(terraform output -raw ecs_repositorio)"
  gh variable set ECS_CLUSTER --body "$(terraform output -raw ecs_cluster)"
  gh variable set ECS_SERVICE --body "$(terraform output -raw ecs_servicio)"
  gh variable set API_ID --body "$(terraform output -raw api_id)"
  gh variable set INTEGRATION_PRESUPUESTOS_COL_ID --body "$(terraform output -raw integracion_presupuestos_coleccion_id)"
  gh variable set INTEGRATION_PRESUPUESTOS_ELE_ID --body "$(terraform output -raw integracion_presupuestos_elemento_id)"
  echo "Variables ECS/API Gateway también sincronizadas."
fi

echo "Listo. Revisa secrets AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY y AWS_SESSION_TOKEN."
