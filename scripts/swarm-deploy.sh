#!/bin/bash
set -e

# Usage: REGISTRY=registry.example.com VERSION=1.0.0 ./scripts/swarm-deploy.sh
REGISTRY=${REGISTRY:?REGISTRY is required (e.g. registry.example.com/myorg)}
VERSION=${VERSION:-latest}
STACK=little-trip

ENV_FILE="$(dirname "$0")/../.env"
if [ -f "$ENV_FILE" ]; then
  set -a; source "$ENV_FILE"; set +a
fi

: "${ADMIN_USERNAME:?ADMIN_USERNAME is required (set in .env)}"
: "${ADMIN_PASSWORD:?ADMIN_PASSWORD is required (set in .env)}"

# ── 1. Build and push the API image ─────────────────────────────────────────
echo "==> Building API image..."
docker build -t "$REGISTRY/little-trip-api:$VERSION" apps/api
docker push "$REGISTRY/little-trip-api:$VERSION"

# ── 2. Deploy infra + API (web excluded for now) ────────────────────────────
echo "==> Deploying infra and API..."
REGISTRY=$REGISTRY VERSION=$VERSION \
  docker stack deploy -c docker-stack.yml "$STACK" --with-registry-auth

# cacheComponents pre-populates "use cache" entries during next build,
# so the API must be reachable at build time.
echo "==> Polling API health endpoint..."
for i in $(seq 1 24); do
  if curl -sf http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "  API is healthy."
    break
  fi
  if [ "$i" -eq 24 ]; then
    echo "ERROR: API did not become healthy in time." >&2
    exit 1
  fi
  echo "  still waiting ($i/24)..."
  sleep 10
done

# ── 3. Fetch API key from admin endpoint ────────────────────────────────────
echo "==> Fetching API key from admin endpoint..."
API_KEY=$(curl -sf \
  -u "${ADMIN_USERNAME}:${ADMIN_PASSWORD}" \
  "http://localhost:8081/api/v1/admin/api-keys?size=1" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['content'][0]['keyHash'])")

if [ -z "$API_KEY" ] || [ "$API_KEY" = "null" ]; then
  echo "ERROR: Failed to fetch API key." >&2
  exit 1
fi
echo "  API key fetched."

# Upsert as a Docker secret (remove old one if it exists)
echo "$API_KEY" | docker secret create api_key_new - 2>/dev/null || true
docker secret rm api_key 2>/dev/null || true
docker secret create api_key <(echo "$API_KEY")
docker secret rm api_key_new 2>/dev/null || true

# ── 4. Build and push the web image (API is now live) ───────────────────────
echo "==> Building web image..."
docker build \
  -t "$REGISTRY/little-trip-web:$VERSION" \
  -f apps/web/Dockerfile \
  .
docker push "$REGISTRY/little-trip-web:$VERSION"

# ── 5. Re-deploy the full stack (now includes web) ──────────────────────────
echo "==> Deploying web..."
REGISTRY=$REGISTRY VERSION=$VERSION \
  docker stack deploy -c docker-stack.yml "$STACK" --with-registry-auth

echo ""
echo "==> Stack deployed."
docker stack services "$STACK"
