#!/usr/bin/env bash
# =============================================================================
# Escribe frontend/public/config.json (ConfigService / loadConfig en runtime).
#
# Entrada, por variables de entorno:
#   REGION  COGNITO_DOMAIN  CLIENT_ID  REDIRECT_URI  API_URL
#
# Opcional:
#   DESTINO   ruta del archivo (por defecto: frontend/public/config.json)
#   EXIGIR_HTTPS=1  falla si REDIRECT_URI no es https (despliegue Amplify)
# =============================================================================

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESTINO="${DESTINO:-$RAIZ/frontend/public/config.json}"

faltan=()
for v in REGION COGNITO_DOMAIN CLIENT_ID REDIRECT_URI API_URL; do
  [[ -n "${!v:-}" ]] || faltan+=("$v")
done
if [[ ${#faltan[@]} -gt 0 ]]; then
  echo "Faltan valores: ${faltan[*]}" >&2
  exit 1
fi

if [[ "${EXIGIR_HTTPS:-0}" == "1" && "$REDIRECT_URI" != https://* ]]; then
  echo "REDIRECT_URI vale '$REDIRECT_URI'; debe ser la URL de Amplify, no localhost." >&2
  exit 1
fi

mkdir -p "$(dirname "$DESTINO")"

python3 - "$DESTINO" <<'PY'
import json, os, sys
path = sys.argv[1]
data = {
    "region": os.environ["REGION"],
    "cognitoDomain": os.environ["COGNITO_DOMAIN"],
    "clientId": os.environ["CLIENT_ID"],
    "redirectUri": os.environ["REDIRECT_URI"],
    "apiUrl": os.environ["API_URL"],
}
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2)
    f.write("\n")
print(f"config.json escrito en {path} (redirectUri: {data['redirectUri']})")
PY
