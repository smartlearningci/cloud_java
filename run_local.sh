#!/usr/bin/env bash
# Phase 1 — levantar serviços localmente (simples)
# Requisitos: Java 21 + Maven
set -e

# 1) tasks-service
( cd tasks-service && mvn -q -DskipTests spring-boot:run ) &

# pequena pausa para dar tempo a arrancar
sleep 5

# 2) gateway (API_KEY é opcional; se definir, o gateway exige o header X-API-KEY)
( cd gateway && API_KEY="${API_KEY:-}" mvn -q -DskipTests spring-boot:run ) &

echo
echo "✔ tasks-service em http://localhost:8081"
echo "✔ gateway       em http://localhost:8080"
[[ -n "${API_KEY:-}" ]] && echo "   * Gateway exige header: X-API-KEY: ${API_KEY}"
echo
echo "Pressiona Ctrl+C para terminar."
wait

