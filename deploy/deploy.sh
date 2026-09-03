#!/usr/bin/env bash

# GitHub Actions가 Lightsail에서 호출하는 배포 스크립트입니다.
# 서버의 .env.production은 Git에 포함하지 않으며, 이 스크립트도 변경하지 않습니다.
set -Eeuo pipefail

readonly APP_DIR="${APP_DIR:-/home/ubuntu/umc-event-fee-webapp}"
readonly COMPOSE_FILE="compose.prod.yaml"
readonly ENV_FILE=".env.production"
readonly TARGET_BRANCH="dev"

cd "$APP_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Deployment aborted: $APP_DIR/$ENV_FILE was not found." >&2
  exit 1
fi

# 서버에서 Git으로 추적되는 파일을 직접 바꿔 둔 경우, 덮어쓰지 않고 안전하게 중단한다.
if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  echo "Deployment aborted: the server working tree has tracked changes." >&2
  echo "Commit or revert those changes on the server before retrying." >&2
  exit 1
fi

git fetch --prune origin "$TARGET_BRANCH"
git switch "$TARGET_BRANCH"
git pull --ff-only origin "$TARGET_BRANCH"

# 1 GB 인스턴스에서도 빌드 중 메모리 사용량이 급증하지 않도록 순차 빌드한다.
COMPOSE_PARALLEL_LIMIT=1 \
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build

echo "Waiting for containers to enter the running state..."
for attempt in {1..12}; do
  running_services="$(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps --status running --services)"

  if grep -qx 'db' <<<"$running_services" &&
    grep -qx 'backend' <<<"$running_services" &&
    grep -qx 'frontend' <<<"$running_services" &&
    grep -qx 'caddy' <<<"$running_services"; then
    break
  fi

  if [[ "$attempt" == '12' ]]; then
    echo "Deployment failed: one or more containers did not start." >&2
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps >&2
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=100 backend frontend caddy >&2
    exit 1
  fi

  sleep 5
done

domain="$(sed -n 's/^DOMAIN=//p' "$ENV_FILE" | head -n 1)"
if [[ -z "$domain" ]]; then
  echo "Deployment failed: DOMAIN is not set in $ENV_FILE." >&2
  exit 1
fi

echo "Checking the public health endpoint..."
for attempt in {1..12}; do
  if curl --fail --silent --show-error --max-time 10 \
    "https://${domain}/api/v1/actuator/health" | grep -q '"status":"UP"'; then
    echo "Deployment completed successfully."
    exit 0
  fi

  sleep 5
done

echo "Deployment failed: the public health endpoint did not become healthy." >&2
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=100 backend frontend caddy >&2
exit 1
