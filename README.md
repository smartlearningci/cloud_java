# README – Phase 4: Containerization and Local Orchestration


### 🎯 Objective of Phase 4

In this phase, the entire system runs in **fully containerized mode**.  
Each microservice is executed inside an isolated **Docker container**, and communication between them is orchestrated by **Docker Compose**, which creates a **shared internal network**.

Additionally:

* the `config-server` reads configuration directly from **GitHub** (`config-repo/`),
* the system scales horizontally with **3 instances of `tasks-service`**,
* the **gateway** performs load balancing across these instances,
* and all services register with **Eureka (Discovery Server)**.

This phase closes the cycle from local to containers, preparing the system for future cloud deployment.

---

### 🛠️ General Architecture (ASCII Diagram)

```
                          +----------------------------------+
                          |           GitHub Repo            |
                          | (contains config-repo with YAMLs) |
                          +----------------------------------+
                                          |
                                          v
                                +-----------------+
                                |  CONFIG SERVER  |
                                |    port 8888    |
                                | Reads configs   |
                                | from GitHub     |
                                +-----------------+
                                          |
                                          v
               +--------------------------------------------------------------+
               |                       DOCKER NETWORK                         |
               |                                                              |
               |  +-----------+       +-------------+       +---------------+  |
               |  | DISCOVERY |<----->|   GATEWAY   |<----->| TASKS-SERVICE |  |
               |  | (Eureka)  |       | (API Edge)  |       |     (x3)      |  |
               |  | Port 8761 |       | Port 8080   |       | Port 8080     |  |
               |  +-----------+       +-------------+       +---------------+  |
               |       ^                                           ^          |
               |       |                                           |          |
               |       +-------------------------------------------+          |
               |               Internal communication (lb://)                 |
               +--------------------------------------------------------------+
```

---

### ⚙️ Execution and Verification Steps

#### 1. Build and Start the System

From the project root:

```bash
docker compose build --parallel
docker compose up -d
```

Wait until all services are **healthy**.

Check running containers:

```bash
docker ps
```

Expected containers:

| Service       | External Port | State   |
| ------------- | ------------- | ------- |
| discovery     | 8761          | healthy |
| config-server | 8888          | healthy |
| gateway       | 8080          | healthy |
| tasks-service | internal only | healthy |

---

#### 2. Scale the Task Service

Run:

```bash
docker compose up -d --scale tasks-service=3
```

Then verify:

```bash
docker ps | grep tasks-service
```

You should see **3 running containers** for `tasks-service`.

---

#### 3. Open the Eureka Dashboard

Open in browser:

👉 `http://localhost:8761`

You should see:

* `GATEWAY`
* `CONFIG-SERVER`
* `TASKS-SERVICE` with **3 instances** registered

Each instance has a unique hostname (container ID).

---

#### 4. Test Gateway and Load Balancing

The **gateway** is the only public entry point. To check load balancing:

```bash
curl http://localhost:8080/api/whoami
```

Repeat several times:

```bash
for i in {1..6}; do curl -s http://localhost:8080/api/whoami; echo; done
```

Expected output:

```
{"service":"tasks-service","hostname":"tasks-service-1","ts":"2025-10-19T18:05:21Z"}
{"service":"tasks-service","hostname":"tasks-service-2","ts":"2025-10-19T18:05:22Z"}
{"service":"tasks-service","hostname":"tasks-service-3","ts":"2025-10-19T18:05:23Z"}
```

✅ The `hostname` changes each time → confirms round-robin load balancing via Gateway.

---

#### 5. Test the Config Server

```bash
curl http://localhost:8888/tasks-service/default
```

Expected: configuration data loaded from GitHub (`config-repo/tasks-service.yml`).

---

#### 6. Check Health Endpoints

| Service                     | URL                                      | Expected Result     |
| --------------------------- | ---------------------------------------- | ------------------- |
| Discovery                   | `http://localhost:8761/actuator/health`  | `{"status":"UP"}`   |
| Config Server               | `http://localhost:8888/actuator/health`  | `{"status":"UP"}`   |
| Gateway                     | `http://localhost:8080/actuator/health`  | `{"status":"UP"}`   |
| Tasks-Service (via Gateway) | `http://localhost:8080/api/whoami` | Valid JSON response |

---

### 📊 Recommended Tests and Observations

1. **Start and scale the system**

   ```bash
   docker compose up -d --build
   docker compose up -d --scale tasks-service=3
   ```

2. **Check registration in Eureka**

   * Open `http://localhost:8761`
   * Ensure 3 `TASKS-SERVICE` instances appear

3. **Test API availability**

   ```bash
   curl http://localhost:8080/api/tasks
   curl http://localhost:8080/api/whoami
   ```

4. **Monitor logs**

   ```bash
   docker compose logs -f gateway
   ```

   Confirm that requests are distributed across `tasks-service-1`, `-2`, and `-3`.

5. **Test resilience**
   Stop one instance:

   ```bash
   docker stop cloud-java-tasks-service-1
   ```

   The system should continue functioning, and Eureka should update instance status accordingly.

---

### 📆 Project Structure (Phase 4)

```
cloud_java/
├── discovery/
│   └── Dockerfile
├── config-server/
│   ├── Dockerfile
│   └── src/main/resources/application.yml
├── gateway/
│   └── Dockerfile
├── tasks-service/
│   └── Dockerfile
├── config-repo/
│   ├── gateway.yml
│   └── tasks-service.yml
├── docker-compose.yml
└── .env
```

---

### 🔙 Phase 4 Results Summary

| Component                             | Status   |
| ------------------------------------- | -------- |
| Config Server (GitHub source)         | ✅ Active |
| All services containerized            | ✅        |
| Internal Docker network functional    | ✅        |
| Horizontal scaling of `tasks-service` | ✅        |
| Load balancing via Gateway            | ✅        |
| Health checks and logs verified       | ✅        |

---

### 🚀 Conclusion

By the end of Phase 4, the Cloud Java system runs fully containerized, featuring:

* centralized configuration from GitHub,
* service discovery and dynamic registration,
* horizontal scalability validated,
* and consistent behavior between local and Docker environments.

This phase establishes the foundation for the upcoming stages:

* **Phase 5:** Persistence with PostgreSQL
* **Phase 6:** Cloud deployment and Kubernetes orchestration.
