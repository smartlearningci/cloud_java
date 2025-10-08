#!/usr/bin/env bash
set -euo pipefail

FILE="docker-compose.phase2.yml"
echo "[phase-2] Stopping stack..."
docker compose -f "$FILE" down -v
