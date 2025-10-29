# Phase 6 → Phase 7 Migration — AKS Architecture (with Config Server and Application Gateway)

## 📘 Overview

The Phase 6 architecture was based on **Docker Compose** for local or VM orchestration, using multiple Spring Boot services (e.g., `config-server`, `tasks-service`), a PostgreSQL container, and local network bridges.

**Phase 7** evolves the system into a **Kubernetes (AKS)** architecture, providing scalability, better network management, and integration with Azure managed services:

- Centralized **Config Server**
- **PostgreSQL** via Azure Database for PostgreSQL
- **Application Gateway (AGIC)** for public access and routing
- **Azure Container Registry (ACR)** for image management

---

## 🔹 Architecture Diagram

```
                          ┌──────────────────────────────┐
                          │      Azure App Gateway       │
                          │     (Public IP 4.175.61.171) │
                          └────────────┬─────────────────┘
                                       │
                         Ingress (AGIC - Ingress Controller)
                                       │
          ┌──────────────────────────────────────────────────────────┐
          │                       AKS Cluster                        │
          │                                                          │
          │  ┌─────────────────────────────┐   ┌──────────────────┐  │
          │  │  config-server Deployment   │   │ tasks-service    │  │
          │  │  (port 8888)                │   │ (port 8081)      │  │
          │  │  fetches git configs        │   │ gets config from │  │
          │  │  exposed as ClusterIP svc   │   │ config-server    │  │
          │  └──────────────┬──────────────┘   └────────┬─────────┘  │
          │                 │                           │            │
          │                 ▼                           ▼            │
          │        http://config-server:8888     jdbc:postgresql://  │
          │                │                            │            │
          │                └──────────┬─────────────────┘            │
          │                           │                              │
          │                   PostgreSQL Azure DB                    │
          │        (pgtasksphase7.postgres.database.azure.com)        │
          └──────────────────────────────────────────────────────────┘
```

---

## 🔹 Key Differences

| Component | Phase 6 | Phase 7 | Note |
|------------|----------|----------|------|
| **Orchestration** | Docker Compose | AKS (Kubernetes) | YAML-based deployment |
| **Config Server** | Local container | AKS Deployment | ClusterIP 8888 |
| **Tasks Service** | Local container | AKS Deployment | Uses config-server |
| **Database** | PostgreSQL container | Azure DB | Managed, SSL required |
| **Ingress / Gateway** | Local Nginx | Azure App Gateway (AGIC) | Public IP routing |
| **Images** | Local Docker | Azure Container Registry | Automated pull/push |

---

## 🔹 Step-by-Step Migration

### 1️⃣ Create Azure Container Registry (ACR)

**Portal:**
1. Create Resource → Container Registry
2. Set name, RG, SKU = *Basic*
3. Enable *Admin user*.

**CLI:**
```bash
az acr create --resource-group phase7-rg --name phase7acr --sku Basic
az acr update --name phase7acr --admin-enabled true
```

---

### 2️⃣ Create AKS Cluster

**Portal:**
- Create Resource → Kubernetes Service
- Node Pool: 1 node, Standard_B2s or higher
- Network: *Azure CNI*
- Attach ACR in Integration tab.

**CLI:**
```bash
az aks create   --resource-group phase7-rg   --name aks-phase7   --node-count 1   --enable-addons monitoring   --attach-acr phase7acr   --generate-ssh-keys
```

---

### 3️⃣ Build and Push Docker Images

```bash
docker build -t phase7acr.azurecr.io/cloud-java-configserver:no-eureka-3 .
docker push phase7acr.azurecr.io/cloud-java-configserver:no-eureka-3

docker build -t phase7acr.azurecr.io/cloud-java-tasksservice:no-eureka-3 .
docker push phase7acr.azurecr.io/cloud-java-tasksservice:no-eureka-3
```

---

### 4️⃣ Deploy via YAML (Free Tier Limitation)

Since the Azure Portal (Free Tier) restricts advanced settings (env vars, probes), we used YAML manifests instead.

**config-server.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
spec:
  replicas: 1
  selector:
    matchLabels:
      app: config-server
  template:
    metadata:
      labels:
        app: config-server
    spec:
      containers:
        - name: config-server
          image: phase7acr.azurecr.io/cloud-java-configserver:no-eureka-3
          ports:
            - containerPort: 8888
---
apiVersion: v1
kind: Service
metadata:
  name: config-server
spec:
  selector:
    app: config-server
  ports:
    - port: 8888
      targetPort: 8888
  type: ClusterIP
```

```bash
kubectl apply -f config-server.yaml
kubectl get pods -l app=config-server
```

---

### 5️⃣ Deploy the Tasks Service

**tasks-service.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tasks-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: tasks-service
  template:
    metadata:
      labels:
        app: tasks-service
    spec:
      containers:
        - name: tasks-service
          image: phase7acr.azurecr.io/cloud-java-tasksservice:no-eureka-3
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: docker
            - name: SPRING_CLOUD_CONFIG_URI
              value: http://config-server.default.svc.cluster.local:8888
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://pgtasksphase7.postgres.database.azure.com:5432/tasksdb?sslmode=require
            - name: SPRING_DATASOURCE_USERNAME
              value: phase7admin
            - name: SPRING_DATASOURCE_PASSWORD
              value: Phase7!Pass123
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 45
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: tasks-service
spec:
  selector:
    app: tasks-service
  ports:
    - port: 8081
      targetPort: 8081
  type: ClusterIP
```

```bash
kubectl apply -f tasks-service.yaml
kubectl get pods -l app=tasks-service
```

---

### 6️⃣ Enable Application Gateway Ingress Controller (AGIC)

**Portal:**
- Go to AKS → Ingress Controllers → Add Controller
- Type: Application Gateway
- Existing Gateway: `appgw-phase7`
- Namespace: `kube-system`
- Enable controller.

**CLI:**
```bash
az aks enable-addons   --addons ingress-appgw   --name aks-phase7   --resource-group phase7-rg   --appgw-name appgw-phase7
```

Check:
```bash
kubectl get pods -n kube-system -l app=ingress-appgw -o wide
```

---

### 7️⃣ Create Ingress Resource

**tasks-ingress.yaml**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tasks-ingress
  annotations:
    kubernetes.io/ingress.class: azure/application-gateway
spec:
  rules:
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: tasks-service
            port:
              number: 8081
```

```bash
kubectl apply -f tasks-ingress.yaml
kubectl get ingress -o wide
```

Expected output:
```
NAME            CLASS    HOSTS   ADDRESS         PORTS   AGE
tasks-ingress   <none>   *       4.175.61.171    80      5m
```

---

### 8️⃣ Test Service

Visit:
```
http://4.175.61.171/actuator/health
```
Expected response:
```json
{"status":"UP"}
```

---

## ✅ Notes and Limitations

- Free tier portal limits YAML complexity (env vars, probes).  
  → Deployments applied using `kubectl apply -f`.
- DNS resolution inside the cluster uses `*.svc.cluster.local`.
- App Gateway (AGIC) automatically manages routes and health checks.
- Each microservice runs in its own deployment, independently scalable.
