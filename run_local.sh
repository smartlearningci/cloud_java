#!/usr/bin/env bash
set -euo pipefail

export CONFIG_SERVER_URL="${CONFIG_SERVER_URL:-http://localhost:8888}"
export EUREKA_URL="${EUREKA_URL:-http://localhost:8761/eureka}"
export API_KEY="${API_KEY:-}"

pids=()

cleanup() {
  echo
  echo "[phase-2] Stopping all Spring Boot processes..."
  for pid in "${pids[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait || true
  echo "[phase-2] Done."
}
trap cleanup INT TERM EXIT

echo "[phase-2] Starting config-server..."
mvn -q -pl config-server -DskipTests spring-boot:run &
pids+=("$!")
sleep 5

echo "[phase-2] Starting discovery (Eureka)..."
mvn -q -pl discovery -DskipTests spring-boot:run &
pids+=("$!")
sleep 5

echo "[phase-2] Starting tasks-service..."
CONFIG_SERVER_URL="$CONFIG_SERVER_URL" EUREKA_URL="$EUREKA_URL" mvn -q -pl tasks-service -DskipTests spring-boot:run &
pids+=("$!")
sleep 8

echo "[phase-2] Starting gateway..."
CONFIG_SERVER_URL="$CONFIG_SERVER_URL" EUREKA_URL="$EUREKA_URL" API_KEY="$API_KEY" mvn -q -pl gateway -DskipTests spring-boot:run &
pids+=("$!")

echo
echo "[phase-2] All services launched."
echo "  Config Server : http://localhost:8888"
echo "  Discovery     : http://localhost:8761"
echo "  tasks-service : http://localhost:8081"
echo "  Gateway       : http://localhost:8080"
echo
echo "Press Ctrl+C to stop."
wait
