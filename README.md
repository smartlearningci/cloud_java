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
