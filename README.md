
# TaskFlow — Phase 2 (Config Server + Service Discovery)

> In Phase 2 we evolve the Phase 1 setup (Gateway + tasks-service) into a **cloud‑native** baseline by adding:
> - **Spring Cloud Config Server** (externalized configuration)
> - **Eureka (Service Discovery)** for logical naming and service‑level load balancing
> - Dual run modes: **local (STS/Maven)** and **Docker Compose**
> - **Actuator** endpoints intentionally exposed in dev for learning/inspection

---

## TL;DR (quick start)

```bash
# Clone and switch to Phase-2 tag
git clone https://github.com/smartlearningci/cloud_java.git
cd cloud_java
git fetch --all --tags
git checkout -b local-phase-2 tags/phase-2

# Docker (recommended for demos)
chmod +x run_compose.sh stop_compose.sh
./run_compose.sh

# Local (no Docker; Java 21 + Maven)
chmod +x run_local.sh
./run_local.sh
```

Service URLs:
- Config Server: `http://localhost:8888`
- Discovery (Eureka): `http://localhost:8761`
- tasks-service: `http://localhost:8081`
- Gateway: `http://localhost:8080`

---

## Repository layout (Phase 2)

```
cloud_java/
├─ pom.xml                      # parent (aggregator) POM
├─ config-repo/                 # external configs (dev: native backend)
│  ├─ gateway.yml
│  └─ tasks-service.yml
├─ config-server/               # Spring Cloud Config Server (8888)
├─ discovery/                   # Eureka Server (8761)
├─ gateway/                     # Spring Cloud Gateway (8080)
├─ tasks-service/               # Tasks API (8081, H2, JPA, Actuator)
├─ docker-compose.yml    # Compose (Phase 2)
├─ run_compose.sh        # Build & start Docker stack
├─ stop_compose.sh       # Stop & clean Docker stack
└─ run_local.sh          # Run all with Maven (no Docker)
```

---

## Architecture (ASCII)

```
Clients (curl/Postman/Browser)
           |
           v
+-----------------+        pulls config       +-----------------------+
|   Gateway (8080)|-------------------------->|   Config Server 8888  |
|  routes /api/*  |                          |  (native -> config-repo)
+-----------------+                          +-----------------------+
        |   \                                        ^
        |    \  lb://tasks-service (via Eureka)      | serves YAML
        |     \                                      |
        v      \                                     |
+----------------------+            registers         |
|  tasks-service 8081  |------------------------------+
|  (H2, JPA, Actuator) |           discovers via Eureka
+----------------------+
             ^
             |  UI/API
      +-----------------+
      | Eureka 8761     |
      | (Discovery)     |
      +-----------------+
```

---

## Core concepts

### Why **Config Server** (12‑Factor: Config)
- Without a config service: `application.yml` is baked into the JAR → rebuilds/redeploys for small changes, weak auditability, risk of leaking secrets, environment drift.
- With Config Server: **externalized configuration**, **profiles** per environment, **single source of truth**, and **immutable artifacts** (same JAR everywhere).

**Dev (training)**: `native` backend (reads from `config-repo/`).  
**Prod**: **Git** backend (GitHub/GitLab/Azure/AWS). **Secrets** must come from **AWS Secrets Manager / SSM** or **Azure Key Vault** — not plain YAML.

**Clients** (`gateway`, `tasks-service`) use a **thin** local `application.yml`:
```yaml
spring:
  application:
    name: tasks-service   # or gateway
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:}
```
- Lookup key: `spring.application.name` → maps to `tasks-service.yml` / `gateway.yml` in the config backend.
- `optional:` lets the service boot even if Config Server is not yet available (handy in dev).

### Why **Discovery (Eureka)**
- Replace host:port coupling with a **logical name** (`tasks-service`).  
- Enables **load balancing** and **failover** across instances.  
- **Gateway** uses `uri: lb://tasks-service` and resolves actual instances through the registry.

**Load Balancer vs Discovery**  
- LB (ALB/NLB/NGINX) distributes traffic across known targets.  
- Discovery provides **dynamic membership** and **naming**; both can coexist (LB can front public traffic; Discovery manages internal service membership).

**Cloud/K8s**  
- In **Kubernetes**, discovery is usually **DNS + Services** (often no Eureka required).  
- **AWS/Azure** alternatives: **AWS Cloud Map/Private DNS**, **Azure Container Apps/Private DNS**.  
- **Hybrid** (on‑prem ↔ cloud): place Config/Eureka in cloud and services on‑prem (or the opposite) via **VPN/peering**; prefer **private endpoints**; lock down Actuator.

---

## How to run

### A) **Docker Compose** (recommended)
```bash
chmod +x run_compose.sh stop_compose.sh
./run_compose.sh
# stop/clean:
./stop_compose.sh
```
> Optional: create `.env` from `.env.example` and set `API_KEY=` to require `X-API-KEY` in the gateway.

### B) **Local (STS/IntelliJ)**
Start in this order: **1) config-server → 2) discovery → 3) tasks-service → 4) gateway**.  
Run/Debug **Environment Variables** for **gateway** and **tasks-service**:
- `CONFIG_SERVER_URL=http://localhost:8888`
- `EUREKA_URL=http://localhost:8761/eureka`
- `API_KEY=` (empty disables the header requirement)

### C) **Local (Maven CLI)**
```bash
mvn -q -pl config-server -DskipTests spring-boot:run
mvn -q -pl discovery     -DskipTests spring-boot:run
CONFIG_SERVER_URL=http://localhost:8888 EUREKA_URL=http://localhost:8761/eureka \
mvn -q -pl tasks-service -DskipTests spring-boot:run
CONFIG_SERVER_URL=http://localhost:8888 EUREKA_URL=http://localhost:8761/eureka API_KEY= \
mvn -q -pl gateway       -DskipTests spring-boot:run
```

---

## Where configuration lives

- `config-repo/tasks-service.yml`: H2/JPA, dev Actuator exposure, `EUREKA_URL`.  
- `config-repo/gateway.yml`: **routes** (note: **new keys** `spring.cloud.gateway.server.webflux.routes`), dev Actuator exposure, `EUREKA_URL`, `app.security.*` (API key).

> **Gateway (Spring Cloud 2025.0.x)**  
> Use the **`spring.cloud.gateway.server.webflux.routes`** prefix.  
> The **Actuator endpoint** remains **`/actuator/gateway`**; ensure it’s included in `management.endpoints.web.exposure.include`.

---

## Inspect at runtime (browser / curl)

**Config Server (served config):**
- JSON (all sources):  
  `http://localhost:8888/tasks-service/default`  
  `http://localhost:8888/gateway/default`
- Raw YAML:  
  `http://localhost:8888/tasks-service-default.yml`  
  `http://localhost:8888/gateway-default.yml`

**Clients (effective config):**
- Environment (filtered):  
  `http://localhost:8081/actuator/env?pattern=spring.datasource.*`  
  `http://localhost:8080/actuator/env?pattern=app.security.*`
- Bound properties:  
  `http://localhost:8081/actuator/configprops`  
  `http://localhost:8080/actuator/configprops`
- **Gateway routes**:  
  `http://localhost:8080/actuator/gateway/routes`

**Eureka (Discovery):**
- UI: `http://localhost:8761` (should list **GATEWAY** and **TASKS-SERVICE** as **UP**)  
- API:
```bash
curl -H "Accept: application/json" http://localhost:8761/eureka/apps
curl -H "Accept: application/json" http://localhost:8761/eureka/apps/TASKS-SERVICE
```

> **Security**: Actuator is **widely exposed for dev**. In prod, **restrict** aggressively.

---

## Tests (exhaustive)

**Health**
```bash
curl -s http://localhost:8888/actuator/health
curl -s http://localhost:8761/actuator/health
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8080/actuator/health
```

**Config (served vs applied)**
```bash
curl -s http://localhost:8888/tasks-service/default | jq .
curl -s http://localhost:8888/gateway/default | jq .
curl -s 'http://localhost:8081/actuator/env?pattern=spring.jpa.*' | jq .
curl -s 'http://localhost:8080/actuator/env?pattern=spring.cloud.gateway.server.webflux.*' | jq .
```

**Gateway & API**
```bash
curl -s http://localhost:8080/actuator/gateway/routes | jq .
curl -i http://localhost:8080/api/tasks
# with API key (if you set API_KEY)
curl -i -H 'X-API-KEY: supersecret' http://localhost:8080/api/tasks
curl -i -H "Content-Type: application/json" -H "X-API-KEY: supersecret" \
     -d '{"title":"Phase 2 OK","description":"demo","projectId":"GW","assignee":"Student"}' \
     http://localhost:8080/api/tasks
```

**Failure drills**
```bash
# stop tasks-service → gateway should return 502/503 after retries/timeouts
curl -i http://localhost:8080/api/tasks

# remove/typo EUREKA_URL or CONFIG_SERVER_URL in a client → observe behavior
# increase logging at runtime
curl -X POST http://localhost:8080/actuator/loggers/org.springframework.cloud.gateway \
     -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
```

---

## Production guidance

- **Config backend**: **Git** (not `native`). Promote via CI/CD and branches/tags.
- **Secrets**: **AWS Secrets Manager/SSM** or **Azure Key Vault**; inject as env vars (never plain YAML).
- **Actuator**: expose minimal set behind **auth/TLS**; optional `management.server.port`.
- **TLS** end‑to‑end; **HA** (replicas for gateway/services; redundant Config Servers behind LB).
- **Refresh**: Spring Cloud Bus (Kafka/Rabbit) or controlled rolling restarts.
- **Discovery**: on **K8s**, prefer platform‑native discovery; keep Eureka mainly for VMs/labs.

---

## Checkpoints

- [ ] `http://localhost:8888/gateway/default` and `.../tasks-service/default` return correctly.  
- [ ] `http://localhost:8761` shows **GATEWAY** and **TASKS-SERVICE** as **UP**.  
- [ ] `http://localhost:8080/actuator/gateway/routes` lists the `tasks` route.  
- [ ] `GET /api/tasks` via gateway → 200 (or 401 if API key is enabled and missing).  
- [ ] `POST /api/tasks` → 201.  
- [ ] `/actuator/env` on clients shows properties coming from Config Server (not local file).

---

## Troubleshooting

- **404 at `/actuator/gateway/routes`** → ensure `gateway` is included in `management.endpoints.web.exposure.include`; restart gateway after config changes.  
- **502/503 from gateway** → `tasks-service` not **UP** in Eureka (check UI/API and service logs).  
- **Client starts “without config”** → missing `CONFIG_SERVER_URL`; with `optional:configserver:` it starts with defaults.  
- **Actuator too open** → intentional in dev; **restrict** in prod.  
- **CORS** (only if called from a browser/frontend) → not relevant for curl/Postman; add CORS at the gateway if needed.

---

## Note on **API Key** (MVP)
- If `API_KEY` is **empty** → gateway **does not** require the header.  
- If `API_KEY` is **set** → gateway requires `X-API-KEY: <value>`.  
- Minimal demo security; in Module IV we’ll evolve to IAM/JWT/TLS.
