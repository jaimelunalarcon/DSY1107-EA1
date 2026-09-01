#!/usr/bin/env bash
# Empaqueta el build estático y lo publica en Amplify (create-deployment + upload + start).
set -euo pipefail

APP_ID="${1:?Uso: publicar-amplify.sh <app_id> [rama] <directorio_build>}"
BRANCH="${2:-main}"
BUILD_DIR="${3:-}"

# Si solo hay 2 args, el segundo es el directorio (rama = main).
if [ -z "$BUILD_DIR" ]; then
  BUILD_DIR="$BRANCH"
  BRANCH="main"
fi

if [ ! -d "$BUILD_DIR" ]; then
  echo "::error::No existe el directorio de build: $BUILD_DIR" >&2
  exit 1
fi

ZIP_FILE="$(mktemp -t amplify-dist.XXXXXX.zip)"
trap 'rm -f "$ZIP_FILE"' EXIT

echo "Empaquetando $BUILD_DIR..."
(cd "$BUILD_DIR" && zip -qr "$ZIP_FILE" .)

echo "Creando deployment en Amplify (app=$APP_ID, branch=$BRANCH)..."
CREATE_JSON="$(aws amplify create-deployment \
  --app-id "$APP_ID" \
  --branch-name "$BRANCH" \
  --output json)"

JOB_ID="$(echo "$CREATE_JSON" | python3 -c "import sys, json; print(json.load(sys.stdin)['jobId'])")"
UPLOAD_URL="$(echo "$CREATE_JSON" | python3 -c "import sys, json; print(json.load(sys.stdin)['zipUploadUrl'])")"

echo "Subiendo artefacto (job $JOB_ID)..."
curl -fsS -X PUT -T "$ZIP_FILE" "$UPLOAD_URL"

aws amplify start-deployment \
  --app-id "$APP_ID" \
  --branch-name "$BRANCH" \
  --job-id "$JOB_ID"

echo "Deploy iniciado. Job: $JOB_ID"
