#!/usr/bin/env bash
set -euo pipefail


urls=(
http://localhost:8761/actuator/health
http://localhost:8888/actuator/health
http://localhost:${GATEWAY_PORT:-8080}/actuator/health
http://localhost:${TASKS_PORT:-8081}/actuator/health
)


for u in "${urls[@]}"; do
echo "checking: $u"
curl -fsS "$u" > /dev/null
echo OK
done


curl -i "http://localhost:${GATEWAY_PORT:-8080}/api/tasks" || true
