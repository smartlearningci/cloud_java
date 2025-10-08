#!/usr/bin/env bash
set -euo pipefail

FILE="docker-compose.phase2.yml"
if [[ ! -f "$FILE" ]]; then
  echo "ERROR: $FILE not found in current directory. Run this from the repo root."
  exit 1
fi

echo "[phase-2] Building and starting stack..."
docker compose -f "$FILE" up --build
