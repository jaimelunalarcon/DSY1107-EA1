#!/usr/bin/env bash
#
# Publica notificaciones-ms o rabbit-admin-ms en ECS (mismo patron que publicar-ecs.sh).
#
#   ./scripts/publicar-ms-ecs.sh notificaciones-ms
#   ./scripts/publicar-ms-ecs.sh rabbit-admin-ms
#
set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TF="terraform -chdir=${RAIZ}/terraform"
MODULO="${1:-}"

uso() {
  echo "Uso: $0 notificaciones-ms|rabbit-admin-ms" >&2
  exit 1
}

[[ "$MODULO" == "notificaciones-ms" || "$MODULO" == "rabbit-admin-ms" ]] || uso

case "$MODULO" in
  notificaciones-ms)
    OUTPUT_ECR=ecr_notificaciones
    OUTPUT_SERVICIO=ecs_servicio_notificaciones
    CONTENEDOR=notificaciones
    JAR=notificaciones-ms.jar
    ;;
  rabbit-admin-ms)
    OUTPUT_ECR=ecr_rabbit_admin
    OUTPUT_SERVICIO=ecs_servicio_rabbit_admin
    CONTENEDOR=rabbit-admin
    JAR=rabbit-admin-ms.jar
    ;;
esac

leer() {
  local desde_entorno="${!1:-}"
  if [ -n "$desde_entorno" ]; then
    echo "$desde_entorno"
  elif command -v terraform >/dev/null 2>&1 && $TF output -raw "$2" >/dev/null 2>&1; then
    $TF output -raw "$2"
  else
    echo "Falta \$$1 y no hay output '$2' en Terraform." >&2
    return 1
  fi
}

REPO="$(leer ECR_REPO "$OUTPUT_ECR")"
CLUSTER="$(leer ECS_CLUSTER ecs_cluster)"
SERVICIO="$(leer ECS_SERVICE "$OUTPUT_SERVICIO")"
VERSION="v$(date +%Y%m%d-%H%M%S)"

command -v jq >/dev/null || { echo "Falta jq (brew install jq)." >&2; exit 1; }
command -v docker >/dev/null || { echo "Falta Docker." >&2; exit 1; }

echo "==> 1/4 Jar ($MODULO)"
( cd "${RAIZ}/${MODULO}" && ./mvnw --batch-mode --no-transfer-progress clean verify )

echo "==> 2/4 Imagen linux/amd64 (${VERSION})"
docker build --platform linux/amd64 \
  -t "${REPO}:${VERSION}" -t "${REPO}:latest" "${RAIZ}/${MODULO}"

echo "==> 3/4 Push ECR"
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "${REPO%%/*}"
docker push "${REPO}:${VERSION}"
docker push "${REPO}:latest"

echo "==> 4/4 Redesplegar servicio $SERVICIO"
# La familia de task definition coincide con el nombre del servicio.
TAREA_JSON="$(aws ecs describe-task-definition --region "$REGION" \
  --task-definition "$SERVICIO" --query taskDefinition --output json \
  | jq --arg imagen "${REPO}:${VERSION}" --arg nombre "$CONTENEDOR" '
      .containerDefinitions = (.containerDefinitions
        | map(if .name == $nombre then .image = $imagen else . end))
      | del(.taskDefinitionArn, .revision, .status, .requiresAttributes,
            .compatibilities, .registeredAt, .registeredBy, .deregisteredAt)')"

REVISION="$(aws ecs register-task-definition --region "$REGION" \
  --cli-input-json "$TAREA_JSON" \
  --query "taskDefinition.taskDefinitionArn" --output text)"

aws ecs update-service --region "$REGION" \
  --cluster "$CLUSTER" --service "$SERVICIO" \
  --task-definition "$REVISION" >/dev/null

echo "    Esperando despliegue..."
for INTENTO in $(seq 1 60); do
  LEIDO="$(aws ecs describe-services --region "$REGION" \
    --cluster "$CLUSTER" --services "$SERVICIO" \
    --query "services[0].[deployments[0].rolloutState,runningCount,desiredCount]" \
    --output text)"
  ESTADO="$(echo "$LEIDO" | awk '{print $1}')"
  CORRIENDO="$(echo "$LEIDO" | awk '{print $2}')"
  DESEADAS="$(echo "$LEIDO" | awk '{print $3}')"
  echo "    ${ESTADO}  ${CORRIENDO}/${DESEADAS}"
  if [ "$ESTADO" = "COMPLETED" ] && [ "$CORRIENDO" = "$DESEADAS" ]; then
    echo "OK: $MODULO en ECS ($REVISION)"
    exit 0
  fi
  if [ "$ESTADO" = "FAILED" ]; then
    echo "Fallo. Logs: aws logs tail /ecs/${SERVICIO} --follow" >&2
    exit 1
  fi
  sleep 10
done
echo "Timeout. Revisa: aws logs tail /ecs/${SERVICIO} --follow" >&2
exit 1
