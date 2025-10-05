#!/usr/bin/env bash
set -euo pipefail

# Build & run both services. This makes the "two-layer" evolution tangible.
docker compose up --build
