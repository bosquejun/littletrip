#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export BASE_URL="${BASE_URL:-http://localhost:8081/api/v1}"
export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}"

echo "Running k6 stress test..."
echo "  BASE_URL: $BASE_URL"
echo "  K6_PROMETHEUS_RW_SERVER_URL: $K6_PROMETHEUS_RW_SERVER_URL"
echo ""

k6 run --out experimental-prometheus-rw "$SCRIPT_DIR/stress-test.js" "$@"
