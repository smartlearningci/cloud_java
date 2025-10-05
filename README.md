# TaskFlow — Phase 0 (Tasks Service)

> **Goal:** a tiny, self-contained REST API for tasks using Spring Boot, Java 21, and an in-memory H2 database.  
> **Why Phase 0?** Start simple so learners can run and succeed immediately. We’ll evolve this service through Phase 1–4 during the course.

---

## 0) Quick facts

- **Tech:** Java 21, Spring Boot 3.5.6, Spring Web, Spring Data JPA, H2, Lombok 1.18.42
- **Port:** `8081`
- **DB:** in-memory H2 (data resets on restart)
- **Endpoints:**  
  - `POST /tasks` — create a task  
  - `GET /tasks` — list tasks (filters: `status`, `projectId`)  
  - `GET /tasks/{id}` — get a specific task  
  - `PATCH /tasks/{id}/status` — change status  
- **First boot:** seeds 2 demo tasks so `GET /tasks` returns data right away
- **Next phases:** Gateway (Phase 1), Config/Discovery/Resilience (Phase 2), Cloud/on-prem deploy (Phase 3), Security/Observability/CI (Phase 4)

---

## 1) Prerequisites

### Java & Maven
- Java **21** (JDK) — `java -version` should show 21
- Maven **3.9+** — `mvn -v`

> If you prefer **Docker only**, you don’t need Java/Maven on your host.

### IDE setup (choose one)

**A) Spring Tools Suite (STS/Eclipse)**
1. `Preferences → Java → Installed JREs` → add **JDK 21** and set as **Default**.
2. Install **Lombok** (compile-time code generation for getters/constructors):
   - Download `lombok-1.18.42.jar`; run: `java -jar lombok-1.18.42.jar`
   - Select your `STS.ini` and click **Install/Update** → **Restart STS**
   - Verify `-javaagent:/…/lombok.jar` in *About STS → Installation Details → Configuration*
3. Import the project: `File → Import → Maven → Existing Maven Projects`
4. Right-click project → **Maven → Update Project** (Alt+F5), then **Project → Clean**

**B) IntelliJ IDEA**
1. Set **Project SDK** to JDK **21**
2. Install **Lombok** plugin and enable **Annotation Processing**:
   - `Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable`
3. Open project (Maven auto-import should configure dependencies)

---

## 🔧 Running the service

You can run the service either **locally with Maven** or **inside Docker**.  
Both options expose the API on [http://localhost:8081](http://localhost:8081).

### ▶️ Option A — Run locally (requires JDK 21 + Maven)

```bash
./run_local.sh
```

This script will:
- Compile the project (`mvn clean package -DskipTests`)
- Start the Spring Boot application (`mvn spring-boot:run`)
- The service becomes available at `http://localhost:8081`

Use this option if you want to **inspect the code and debug in your IDE**.

---

### 🐳 Option B — Run with Docker (no JDK/Maven needed on host)

```bash
./run_docker.sh
```

This script will:
- Build a Docker image using the provided `Dockerfile`
- Run the container exposing port `8081`
- The service will be available at `http://localhost:8081`

Example test:
```bash
curl -s http://localhost:8081/tasks | jq
```

Use this option if you want a **zero-setup environment** for learners — only Docker is required.

---

## 2) Try it (curl)

```bash
# List tasks (includes seeded examples on first boot)
curl -s http://localhost:8081/tasks | jq

# Create a task
curl -s -H "Content-Type: application/json"   -d '{"title":"First task","description":"demo"}'   -X POST http://localhost:8081/tasks | jq

# Get by id (replace <ID>)
curl -s http://localhost:8081/tasks/<ID> | jq

# Update status
curl -s -H "Content-Type: application/json"   -d '{"status":"DONE"}'   -X PATCH http://localhost:8081/tasks/<ID>/status | jq

# Health
curl -s http://localhost:8081/actuator/health | jq
```

---

## 3) API reference

### JSON model (Task)
```json
{
  "id": "uuid-string",
  "title": "string (required on create)",
  "description": "string|null",
  "status": "TODO|DOING|DONE",
  "projectId": "string|null",
  "assignee": "string|null",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### Endpoints

**POST `/tasks`** — *Create*  
**Body**
```json
{ "title": "Write docs", "description": "Phase 0 README", "projectId": "P1" }
```
**201 Created** → returns full Task JSON.

**GET `/tasks?status=&projectId=`** — *List*  
Filters are optional. Examples:
- `/tasks` (all)
- `/tasks?status=TODO`
- `/tasks?projectId=P1`
- `/tasks?status=DOING&projectId=P1`

**GET `/tasks/{id}`** — *Get by id*  
**200** with Task JSON or **404**.

**PATCH `/tasks/{id}/status`** — *Update status*  
**Body**
```json
{ "status": "DONE" }
```
**200** with updated Task or **404** if not found.

---

## 4) Troubleshooting

- **Whitelabel / generic 500** → We provide a **JSON error handler**. Check runtime logs for details.
- **“Ensure the compiler uses the `-parameters` flag”** → The project’s POM enables this via `maven-compiler-plugin`. Make sure your IDE builds with Maven, or run `mvn clean compile`.
- **Lombok getters/constructors missing in IDE** → Your IDE isn’t loading Lombok:
  - STS: reinstall Lombok (via `lombok.jar`), then restart STS
  - IntelliJ: install Lombok plugin + enable annotation processing
- **Java 1.8 vs 21 mismatch** → In STS set project JRE to **JavaSE-21** (Build Path + Compiler compliance = 21).
- **H2 console** (optional) → We enable it on `/h2-console`. Use JDBC URL `jdbc:h2:mem:tasks`.

---

## 5) What’s next (Phase 1)

We’ll add a **Gateway** (Spring Cloud Gateway) in front of this service, route `/api/tasks/** → tasks-service`, optionally enforce an API key, and package both with **Docker Compose**. We’ll tag Git commits by phase so learners can browse versions easily.

---




## 🧭 Phase 1 — Adding an API Gateway (Evolution, Not Replacement)

In **Phase 0**, the `tasks-service` exposed a REST API directly at  
`http://localhost:8081/tasks`.

In **Phase 1**, we **introduce a new layer** — a **Spring Cloud Gateway** — that becomes the **single entry point** for all clients.

This makes the evolution explicit:  
the existing backend remains unchanged and fully functional, but now it’s accessed **through the gateway** (`http://localhost:8080/api/tasks/**`).

---

### 🧱 Architecture at this stage
```
Client → Gateway (port 8080) → tasks-service (port 8081)
```

- The gateway forwards every request from `/api/tasks/**`  
  to the backend’s `/tasks/**` endpoint.
- The backend still runs on its own port (8081) and can be accessed directly for comparison.
- Both services are orchestrated with Docker Compose.

---

### ▶️ How to Run (two options)

#### 🧩 Option A — Run inside Spring Tool Suite (STS)
1. Launch **`tasks-service`** (as Spring Boot App).  
   It will start on port 8081.
2. Launch **`gateway`** (as Spring Boot App).  
   It will start on port 8080.
3. Test using the commands below.

#### 🐳 Option B — Run with Docker Compose (recommended)
From the project root (`cloud_java`):

```bash
./run_compose.sh
# or
docker compose up --build
```

This builds both images and runs the containers in the same network.

---

### 🔍 How to Test

#### 1️⃣ Access through the Gateway (new entry point, port 8080)
```bash
# List all tasks (GET)
curl -s http://localhost:8080/api/tasks | jq

# Create a new task (POST)
curl -s -H "Content-Type: application/json"   -d '{"title":"Task via Gateway","description":"demo","projectId":"P1","assignee":"Ana"}'   http://localhost:8080/api/tasks | jq

# Update status of a task (PATCH)
curl -s -X PATCH -H "Content-Type: application/json"   -d '{"status":"DOING"}'   http://localhost:8080/api/tasks/<TASK_ID>/status | jq
```

#### 2️⃣ Direct Access (old behaviour still visible, port 8081)
```bash
curl -s http://localhost:8081/tasks | jq
```

> This demonstrates that the old architecture still works —
> the gateway simply adds a new layer on top.

---

### 🔐 (Optional) Enable API Key Authentication
You can enable a simple header check for demo purposes:

1. Define the environment variable before starting:
   ```bash
   export API_KEY=supersecreto
   ```
2. Start Docker Compose (or restart the gateway).
3. Call the API through the gateway:
   ```bash
   curl -s -H "X-API-KEY: supersecreto" http://localhost:8080/api/tasks | jq
   ```

If the header is missing or invalid, the gateway will reply with **401 Unauthorized**.

---

### 🧩 Health Checks
Both services expose health endpoints for monitoring:
- Gateway: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)  
- Tasks-service: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)

These are also used by Docker Compose to wait until the backend is ready before the gateway starts routing.

---

### 🧰 What You Learn Here
- How to add a **gateway layer** on top of an existing backend.  
- How to **compose multiple services** locally with Docker.  
- How to keep previous phases intact while revealing the **evolution of complexity**.

---

### ⏭️ Next Steps (preview of Phase 2)
In the next phase we’ll:
- Externalize configuration via **Spring Cloud Config Server**.
- Add **Service Discovery (Eureka)** for dynamic routing (`lb://tasks-service`).
- Introduce **Resilience4j** for retries and circuit breakers.

---

👉 **Tip:** always test both entry points (8080 and 8081) to reinforce the learning concept —  
each phase *adds a layer* but *never hides the foundation*.
