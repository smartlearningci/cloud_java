#!/usr/bin/env bash
set -e
docker build -t taskflow/tasks-service:phase0 ./tasks-service
docker run --rm -p 8081:8081 taskflow/tasks-service:phase0
