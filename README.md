# TaskFlow — Phase 3 (Load Balancer, Resilience & Logging)

## 1. Introduction
Phase 3 builds upon Phase 2 by introducing **Load Balancing**, **Resilience (Resilience4j)**, and **Structured Logging & Correlation IDs**.  
This phase demonstrates how a cloud‑native architecture behaves when multiple instances are running behind a Gateway using **Spring Cloud LoadBalancer** and **Eureka**.  
We also ensure robust observability and error handling.

---

## 2. Getting the Code
### 🧭 Clone the Repository
```bash
git clone https://github.com/smartlearningci/cloud_java.git
cd cloud_java
git fetch --all --tags
git checkout -b local-phase-3 tags/phase-3
```

### 🧱 Project Structure
```
config-server/
discovery/
gateway/
tasks-service/
config-repo/
docker-compose.phase2.yml
docker-compose.phase3.yml (optional)
```

---

## 3. Architecture Overview (Phase 3)
```
         Clients (curl/Postman)
                  |
                  v
        +-------------------+
        | Gateway (8080)    |
        | lb://tasks-service|
        +---------+---------+
                  |
       -----------------------
       |                     |
+---------------+    +---------------+
| tasks-service |    | tasks-service |
| (8081)        |    | (8082)        |
+---------------+    +---------------+
       ^                     ^
       |                     |
  +------------+      +------------+
  |  Eureka    |      | Config     |
  |  8761      |      | 8888       |
  +------------+      +------------+
```

---

## 4. Docker & Compose Setup for Phase 3

Phase 3 uses the same base setup as Phase 2 but adds **scaling** and **multi‑instance discovery**.

### 🧩 What Stays the Same
- Dockerfiles for all modules (config‑server, discovery, gateway, tasks‑service).  
- Same Config Server and Eureka setup.  
- Gateway uses `lb://tasks-service` URI.  
- Resilience4j and logging are code‑only changes.

### 🔁 What Changes in Phase 3
#### 4.1 Scaling the Task Service
Instead of duplicating YAML entries, use Docker Compose scaling:
```bash
docker compose -f docker-compose.phase2.yml up --build -d
docker compose -f docker-compose.phase2.yml up -d --scale tasks-service=2
```

#### 4.2 Eureka Inside Docker
Make sure instances **don’t register with localhost**.  
Inside containers, use hostnames instead:
```yaml
eureka:
  instance:
    prefer-ip-address: false
    instance-id: ${spring.application.name}:${random.value}
```

#### 4.3 Compose Example (phase3)
```yaml
services:
  config-server:
    build: ./config-server
    ports: ["8888:8888"]

  discovery:
    build: ./discovery
    ports: ["8761:8761"]
    environment:
      - CONFIG_SERVER_URL=http://config-server:8888

  tasks-service:
    build: ./tasks-service
    environment:
      - CONFIG_SERVER_URL=http://config-server:8888
      - EUREKA_URL=http://discovery:8761/eureka
    # No external ports needed for replicas

  gateway:
    build: ./gateway
    ports: ["8080:8080"]
    environment:
      - CONFIG_SERVER_URL=http://config-server:8888
      - EUREKA_URL=http://discovery:8761/eureka
      - API_KEY=${API_KEY}
    depends_on:
      - discovery
      - tasks-service
```

#### 4.4 Start and Scale
```bash
docker compose -f docker-compose.phase3.yml up --build -d
docker compose -f docker-compose.phase3.yml up -d --scale tasks-service=2
```

#### 4.5 Test and Observe
- **Eureka UI:** http://localhost:8761 → shows 2 instances of `TASKS-SERVICE`.
- **Gateway Round Robin:**
  ```bash
  for i in {1..10}; do curl -s http://localhost:8080/api/tasks >/dev/null; done
  ```
  Check logs — requests alternate between instances.

- **Stop one instance:**
  ```bash
  docker compose ps
  docker compose stop <container_name_of_tasks-service_2>
  ```
  Expected: Gateway continues routing to the healthy instance.

#### ⚠️ Common Pitfalls
| Issue | Cause | Fix |
|-------|--------|------|
| `localhost` in Eureka registry | Container advertises itself as `localhost` | Use `prefer-ip-address: false` |
| Replicas not reachable | Wrong URLs (use internal Docker hostnames) | Keep URIs like `http://discovery:8761` |
| LB not working | Gateway URI not using `lb://` | Correct to `lb://tasks-service` |
| Port conflicts | Mapped multiple replicas | Don’t expose replicas externally |

---

## 5. Phase 3A — Load Balancer
Explains Round Robin, Weighted and Random strategies.  
Spring Cloud LoadBalancer defaults to Round Robin.

Expected results:
- Two instances alternate requests.
- Logs show different instance ports (8081 / 8082).

---

## 6. Phase 3B — Resilience (Resilience4j)
Implements timeout, retry and circuit breaker patterns.

Expected results:
- Temporary service failure triggers retries.
- Circuit breaker opens after repeated failures.
- Actuator shows breaker states.

---

## 7. Phase 3C — Structured Logging & Correlation
Adds correlation IDs (MDC) for request tracing.

Expected results:
- Each request in Gateway and Tasks logs shares same `corrId`.
- Errors logged with context.

---

## 8. Tests (Windows & Linux)
Windows 10+ supports curl natively.

### Health checks
```bash
curl -s http://localhost:8088/actuator/health
curl -s http://localhost:8761/actuator/health
curl -s http://localhost:8080/actuator/health
```

### Create and list tasks
```bash
curl -i -H "Content-Type: application/json" -d '{"title":"Phase 3 Test"}' http://localhost:8080/api/tasks
curl -i http://localhost:8080/api/tasks
```

Expected: alternating instance responses, health “UP”.

---

## 9. 12‑Factor Mapping (Phase 3)
| Principle | How it’s applied |
|------------|------------------|
| Codebase | One repo, tagged by phase |
| Dependencies | Managed via Maven per service |
| Config | Externalized via Config Server |
| Backing services | DB & Discovery treated as attached resources |
| Build, release, run | Immutable Docker images |
| Processes | Stateless services |
| Port binding | Each service exposes its own port |
| Concurrency | Achieved via scaling (Compose `--scale`) |
| Disposability | Fast startup/shutdown (H2, Eureka auto-reconnect) |
| Dev/prod parity | Same code runs locally and in Compose |
| Logs | Structured logs w/ correlation ID |
| Admin processes | Observability via Actuator |

---

## 10. Next Steps — Phase 4
Phase 4 will introduce:  
- IAM & JWT authentication;  
- Metrics (Prometheus + Grafana);  
- CI/CD pipelines (GitHub Actions);  
- TLS hardening and secrets management.

