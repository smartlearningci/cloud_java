.PHONY: build up down logs ps test tag
build:
docker compose build --parallel
up:
docker compose up -d
logs:
docker compose logs -f --tail=200
ps:
docker compose ps
down:
docker compose down -v
test:
bash scripts/smoke.sh
