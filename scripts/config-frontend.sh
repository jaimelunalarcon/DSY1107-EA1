#!/usr/bin/env bash
# Genera frontend/src/config.ts desde variables de entorno (CI o sincronizar-github.sh).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_FILE="${1:-$ROOT/frontend/src/config.ts}"

: "${REGION:?Falta REGION (variable GitHub: AWS_REGION)}"
: "${COGNITO_DOMAIN:?Falta COGNITO_DOMAIN (variable GitHub: COGNITO_DOMAIN)}"
: "${CLIENT_ID:?Falta CLIENT_ID (variable GitHub: COGNITO_CLIENT_ID)}"
: "${API_URL:?Falta API_URL (variable GitHub: API_URL)}"
: "${USER_POOL_ID:?Falta USER_POOL_ID (variable GitHub: COGNITO_USER_POOL_ID)}"

DOMAIN="${COGNITO_DOMAIN#https://}"
DOMAIN_URL="https://${DOMAIN}"

mkdir -p "$(dirname "$CONFIG_FILE")"

cat >"$CONFIG_FILE" <<EOF
export const cognitoConfig = {
  authority:
    "https://cognito-idp.${REGION}.amazonaws.com/${USER_POOL_ID}",
  clientId: "${CLIENT_ID}",
  // Local (Vite) o Amplify: debe coincidir con un callback_url de Cognito.
  redirectUri: \`\${window.location.origin}/\`,
  responseType: "code" as const,
  scope: "openid email profile",
  domain: "${DOMAIN_URL}",
  region: "${REGION}",
};

export const apiConfig = {
  baseUrl: "${API_URL}",
};
EOF

echo "Config escrito en $CONFIG_FILE"
