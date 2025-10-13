# TaskFlow — Phase 3: Load Balancing, Resilience, and Structured Logging

This phase extends our **TaskFlow microservices** architecture (Gateway + Tasks-Service + Config + Eureka)
to include **load balancing**, **resilience patterns**, and **structured logging with correlation IDs**.
It builds on Phase 2 (Config Server & Service Discovery) and brings us closer to real-world cloud‑native robustness.

---

## 1. Get the Code (Windows & Linux)

### 🧩 Clone the repository

```bash
# Clone the main repo
git clone https://github.com/smartlearningci/cloud_java.git
cd cloud_java

# Fetch all tags and checkout Phase 3
git fetch --all --tags
git checkout -b local-phase-3 tags/phase-3
```

> 💡 **Note:** You can verify that the branch is detached from tag `phase-3` by running `git status`.

### 🗂 Project structure

```
cloud_java/
│
├── config-server/        → Centralized configuration (Spring Cloud Config)
├── discovery/            → Eureka service registry
├── gateway/              → API gateway (Spring Cloud Gateway)
├── tasks-service/        → Business service (H2 + JPA)
├── config-repo/          → Externalized configs (gateway.yml, tasks-service.yml, etc.)
├── docker-compose.yml    → Local Compose orchestration
└── README_Phase3_FULL.md → This guide
```

---

## 2. Architecture Overview

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
|  tasks-service 8082  | (second instance)            |
|  (H2, JPA, Actuator) |           discovers via Eureka
+----------------------+
             ^
             |  UI/API
      +-----------------+
      | Eureka 8761     |
      | (Discovery)     |
      +-----------------+
```

### How to run (Linux / macOS)
```bash
mvn -q -pl config-server -DskipTests spring-boot:run
mvn -q -pl discovery     -DskipTests spring-boot:run
CONFIG_SERVER_URL=http://localhost:8888 EUREKA_URL=http://localhost:8761/eureka mvn -q -pl tasks-service -DskipTests spring-boot:run
CONFIG_SERVER_URL=http://localhost:8888 EUREKA_URL=http://localhost:8761/eureka mvn -q -pl gateway -DskipTests spring-boot:run
```

### On Windows (PowerShell)
```powershell
set CONFIG_SERVER_URL=http://localhost:8888
set EUREKA_URL=http://localhost:8761/eureka
mvn -q -pl config-server -DskipTests spring-boot:run
mvn -q -pl discovery     -DskipTests spring-boot:run
mvn -q -pl tasks-service -DskipTests spring-boot:run
mvn -q -pl gateway       -DskipTests spring-boot:run
```

✅ **Windows includes `curl`** by default (PowerShell 5+). All test commands below will work natively.

---

## 3. Section A — Load Balancing

### 🧠 Theory

A **Load Balancer** distributes requests across multiple instances of a service to improve scalability and fault tolerance.

**Why it matters:**
- Prevents overload on a single instance.  
- Increases availability and throughput.  
- Enables rolling updates and horizontal scaling.  

**Common algorithms:**
- **Round Robin** — each request goes to the next instance in rotation.  
- **Random** — selects an instance at random.  
- **Least Connections** — chooses the instance with the fewest active connections.  
- **Weighted** — distributes based on instance weights (capacity).  

Spring Cloud integrates a **client-side load balancer** through `Spring Cloud LoadBalancer`, replacing Ribbon.

### ⚙️ Implementation Summary

- Multiple `tasks-service` instances (8081, 8082).  
- Gateway routes requests using `lb://tasks-service` URI.  
- Discovery handled via Eureka registry.  

**Main config files:**
- `config-repo/gateway.yml` — declares `uri: lb://tasks-service`.
- `config-repo/tasks-service.yml` — defines unique `server.port` for each instance.

### 🧪 Test & Expected Result

```bash
curl -i http://localhost:8080/api/tasks
curl -i http://localhost:8080/api/tasks
curl -i http://localhost:8080/api/tasks
```

Expected:
- Responses alternate between `8081` and `8082` in logs → **Round Robin** verified.

### 🧩 12-Factor Mapping

| Principle | Implementation |
|------------|----------------|
| **Disposability** | Stateless services allow quick start/stop of instances. |
| **Concurrency** | Scale horizontally by adding more instances. |
| **Config** | Externalized configuration (per instance). |

---

## 4. Section B — Resilience (Resilience4j)

### 🧠 Theory

**Resilience patterns** protect services from cascading failures.  
Implemented using [Resilience4j](https://resilience4j.readme.io).

Core patterns:
1. **Timeouts** — avoid hanging threads when a dependency is slow.  
2. **Retries** — retry failed calls a few times before giving up.  
3. **Circuit Breaker** — opens the circuit after repeated failures, preventing overload.  

**Circuit Breaker states:**
```
[Closed] → normal traffic
  | failures >
[Open] → short-circuits requests
  | after wait period
[Half-Open] → test if service recovered → back to Closed if OK
```

### ⚙️ Implementation Summary

- Added `TaskServiceCommand` wrapper for outbound calls with `@CircuitBreaker`, `@Retry`, and `@TimeLimiter`.  
- Configured in `application.yml` under `resilience4j.*`.  
- Uses `fallback()` methods for controlled degradation.  

### 🧪 Test & Expected Result

**Simulate failure:**
```bash
# Stop one instance (e.g., 8081)
curl -i http://localhost:8080/api/tasks
```

Expected:
- Circuit opens after repeated failures (`logs: CircuitBreaker[taskService] -> OPEN`).
- Requests return `503` instead of hanging.  

### 🧩 12-Factor Mapping

| Principle | Implementation |
|------------|----------------|
| **Dev/Prod parity** | Same config patterns across envs. |
| **Disposability** | Services degrade gracefully instead of failing. |
| **Telemetry** | Built-in metrics for health and latency. |

---

## 5. Section C — Structured Logging & Correlation IDs

### 🧠 Theory

Logging is the nervous system of distributed systems.

**Challenges in microservices:**
- One user action may trigger many service calls.  
- Tracing without correlation is hard.  

**Solution:** use a **Correlation ID** per request and propagate it across services.

We achieve this via:
- `MDC` (Mapped Diagnostic Context) → stores correlationId in the logging context.  
- A custom `CorrelationGlobalFilter` in Gateway injects/propagates the ID.  
- `LoggingConfig` defines structured log format.

### ⚙️ Implementation Summary

Key classes:
- `CorrelationGlobalFilter` → adds header `X-Correlation-Id`, logs inbound/outbound.  
- `LoggingConfig` → defines console pattern (timestamp, corrId, level, message).  

**Log format:**
```
2025-10-12 23:39:20,702 corrId=42a24c41-24dc-4935-9ecd-541cd6d49ea0 INFO --- GW IN GET /api/tasks
2025-10-12 23:39:20,880 corrId=42a24c41-24dc-4935-9ecd-541cd6d49ea0 INFO --- GW OUT status=200 OK
```

### 🧪 Test & Expected Result

```bash
curl -i http://localhost:8080/api/tasks
```

Expected:
- Same `corrId` appears in all logs through Gateway → trace end-to-end request flow.

### 🧩 12-Factor Mapping

| Principle | Implementation |
|------------|----------------|
| **Logs** | Structured, contextual logs (machine-parsable). |
| **Telemetry** | Correlation improves observability. |
| **Disposability** | Logs detached from runtime (stdout). |

---

## 6. Full Test Suite (curl examples)

### Health checks
```bash
curl -s http://localhost:8888/actuator/health
curl -s http://localhost:8761/actuator/health
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8080/actuator/health
```

### Config visibility
```bash
curl -s http://localhost:8888/tasks-service/default | jq .
curl -s http://localhost:8888/gateway/default | jq .
```

### Gateway routing & LB
```bash
for i in {1..6}; do curl -s http://localhost:8080/api/tasks | jq '.[] | .id'; done
```

Expected: requests alternate between both instances.

### Circuit breaker demo
```bash
# Stop tasks-service:8081
curl -i http://localhost:8080/api/tasks
```

Expected: 503 errors → circuit opens.

### Correlation log trace
```bash
curl -i http://localhost:8080/api/tasks
```

Expected: same corrId in Gateway logs.

---

## 7. 12-Factor Summary Table

| Principle | Implementation in Phase 3 |
|------------|----------------------------|
| **Codebase** | Shared repo with tagged versions per phase |
| **Dependencies** | Managed via Maven |
| **Config** | Centralized in Config Server |
| **Backing services** | H2 database, externalized |
| **Build, release, run** | Deterministic via Compose & Maven |
| **Processes** | Stateless microservices |
| **Port binding** | Each service on own port |
| **Concurrency** | Horizontal scaling via multiple instances |
| **Disposability** | Graceful shutdown, resilience patterns |
| **Dev/Prod parity** | Config profiles (native vs Git) |
| **Logs** | Structured, correlation-aware |
| **Admin processes** | Managed via Actuator endpoints |

---

## 8. Next Steps — Phase 4 Preview

- 🔐 **Security** — API keys, OAuth2, JWT propagation  
- 📈 **Observability** — Prometheus, Grafana dashboards  
- 🚀 **CI/CD** — automated build/test/deploy pipelines

---

**Smart Learning — Cloud‑Native Dev Training 2025**  
_Module III — Phase 3: Load Balancing, Resilience & Logging_
