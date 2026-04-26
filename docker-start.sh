#!/bin/bash
set -e

ENV_FILE="$(dirname "$0")/.env"
if [ -f "$ENV_FILE" ]; then
  set -a; source "$ENV_FILE"; set +a
fi

: "${ADMIN_USERNAME:?ADMIN_USERNAME is required (set in .env)}"
: "${ADMIN_PASSWORD:?ADMIN_PASSWORD is required (set in .env)}"

echo "==> Starting infrastructure and API..."
docker compose up -d --build postgres redis api prometheus redis-exporter grafana

echo "==> Waiting for API to be healthy..."
until docker compose exec api wget -qO- http://localhost:8081/actuator/health > /dev/null 2>&1; do
  sleep 5
  echo "  still waiting..."
done

echo "==> Fetching API key from admin endpoint..."
API_RESPONSE=$(curl -s \
  -u "${ADMIN_USERNAME}:${ADMIN_PASSWORD}" \
  "http://localhost:8081/api/v1/admin/api-keys?size=1")

if [ -z "$API_RESPONSE" ]; then
  echo "ERROR: Empty response from admin endpoint. Check ADMIN_USERNAME/ADMIN_PASSWORD." >&2
  exit 1
fi

API_KEY=$(echo "$API_RESPONSE" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['content'][0]['keyHash'])" 2>/dev/null)

if [ -z "$API_KEY" ] || [ "$API_KEY" = "null" ]; then
  echo "ERROR: Failed to parse API key. Response was:" >&2
  echo "$API_RESPONSE" >&2
  exit 1
fi

export API_KEY
sed -i "s|^API_KEY=.*|API_KEY=\"$API_KEY\"|" "$(dirname "$0")/apps/web/.env"
echo "  API key fetched and written to apps/web/.env."

echo "==> Building and starting web..."
docker compose up -d --build web

echo ""
echo "==> All services running."
docker compose ps
