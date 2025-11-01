# 🚀 Phase 7 — Full Azure AKS Deployment (with App Gateway + AGIC)

```
                ┌─────────────────────────────┐
                │ Azure Application Gateway   │
                │  (Public IP + Listener 80)  │
                └─────────────┬───────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  AKS Cluster    │
                    │   (aks-phase7)  │
                    └─────────────────┘
                       │           │
        ┌───────────────┘           └───────────────┐
        ▼                                           ▼
┌────────────────────┐                    ┌────────────────────┐
│ config-server Pod  │                    │ tasks-service Pod  │
│ Port 8888          │                    │ Port 8081          │
│ Reads Git Config   │                    │ Uses DB tasksbd    │
└────────────────────┘                    └────────────────────┘
                                                │
                                                ▼
                                    ┌────────────────────────────┐
                                    │ Azure PostgreSQL (Flexible)│
                                    │ Server: pgtaskphase7       │
                                    │ Database: tasksbd          │
                                    └────────────────────────────┘
```

---

## ⚙️ 1) Concepts & Theory — Kubernetes and AKS

**Kubernetes:**  
- Core components: *Pods, ReplicaSets, Deployments, Services, Ingress, ConfigMaps/Secrets, health probes* (`startup`, `readiness`, `liveness`).  
- Declarative model: YAML defines desired state → reconciled by controllers.

**AKS (Azure Kubernetes Service):**  
- Managed Kubernetes integrating **Azure Container Registry (ACR)**, **Azure CNI**, **Application Gateway Ingress Controller (AGIC)**, and **Azure Monitor**.  
- Uses *Managed Identity* and supports *RBAC*.  

---

## 🔄 2) From Phase 6 → Phase 7 — What Changed

| Component | Phase 6 | Phase 7 | Notes |
|------------|----------|----------|-------|
| Orchestration | Docker Compose | AKS (Kubernetes) | Declarative YAML, scalable |
| Config Server | Local container | AKS Deployment + Service | Git-backed config |
| API Service | Local container | AKS Deployment + Service | Reads from config-server |
| Database | Local container | Azure PostgreSQL Flexible | SSL required |
| Public Entry | Nginx/App Service | App Gateway + AGIC | Layer 7 routing |
| Registry | Docker Hub/Local | Azure Container Registry | Integrated with AKS |
| Secrets | Inline env vars | K8s Secrets / Key Vault | Improved security |
| Networking | Docker bridge | Azure CNI | Pod IPs inside VNet |

---

## 🧭 3) End-to-End Deployment Guide (Phase 7)

### Region: West Europe  
### Resource Group: `phase7`  
### Components

| Service | Name |
|----------|------|
| **VNet/Subnets** | `vnet-phase7`, `aks-subnet`, `appgw-subnet` |
| **Application Gateway** | `appgw-phase7` + IP `appw-phase7-ip` |
| **AKS Cluster** | `aks-phase7` |
| **ACR** | `phase7registry` |
| **Database** | `pgtaskphase7.postgres.database.azure.com` |
| **DB name (app)** | `tasksbd` |
| **Credentials** | user: `phase7admin`, pass: `Phase7!Pass123` |
| **Images** | `phase7registry.azurecr.io/cloud-java-configserver:no-eureka-3`<br>`phase7registry.azurecr.io/cloud-java-tasksservice:no-eureka-3` |

---

### 0️⃣ First-time Azure CLI setup

```bash
az login
az account list -o table
az account set --subscription "<YOUR-SUBSCRIPTION-NAME>"

# Register providers
az provider register --namespace Microsoft.Network
az provider register --namespace Microsoft.ContainerService
az provider register --namespace Microsoft.ContainerRegistry
az provider register --namespace Microsoft.OperationalInsights
az provider register --namespace Microsoft.Insights

# Confirm all Registered
az provider list --query "[].{ns:namespace, state:registrationState}" -o table
```

---

### 1️⃣ Resource Group & Virtual Network

```bash
az group create -n phase7 -l westeurope

az network vnet create -g phase7 -n vnet-phase7   --address-prefix 10.0.0.0/8   --subnet-name aks-subnet --subnet-prefix 10.1.0.0/16

az network vnet subnet create -g phase7 --vnet-name vnet-phase7   -n appgw-subnet --address-prefix 10.2.0.0/16
```

---

### 2️⃣ Application Gateway

Portal → Create → **Application Gateway**

- RG = `phase7`
- Name = `appgw-phase7`
- Tier = Standard v2  
- Region = West Europe  
- VNet = `vnet-phase7`, Subnet = `appgw-subnet`
- IP = new static public IP → `appw-phase7-ip`
- Autoscale = Min 1 / Max 2
- Backend Pool = placeholder
- Listener = Port 80 HTTP
- Routing Rule = link listener → pool (backend port 8081, timeout 30 s)

CLI equivalent:

```bash
az network public-ip create -g phase7 -n appw-phase7-ip --sku Standard --allocation-method Static

az network application-gateway create   -g phase7 -n appgw-phase7   --vnet-name vnet-phase7   --subnet appgw-subnet   --sku Standard_v2   --capacity 1   --public-ip-address appw-phase7-ip

az network application-gateway update -g phase7 -n appgw-phase7   --min-capacity 1 --max-capacity 2
```

---

### 3️⃣ Azure Container Registry + Multi-Arch Build

```bash
az acr create -g phase7 -n phase7registry --sku Basic --admin-enabled true
az acr login -n phase7registry

docker buildx create --use
docker buildx inspect --bootstrap

# Config Server
docker buildx build   -f ./config-server/Dockerfile   -t phase7registry.azurecr.io/cloud-java-configserver:no-eureka-3   --platform linux/amd64,linux/arm64 --push .

# Tasks Service
docker buildx build   -f ./tasks-service/Dockerfile   -t phase7registry.azurecr.io/cloud-java-tasksservice:no-eureka-3   --platform linux/amd64,linux/arm64 --push .

az acr repository list -n phase7registry -o table
az acr repository show-tags -n phase7registry --repository cloud-java-tasksservice -o table
```

---

### 4️⃣ AKS Cluster (Azure CNI + ACR Attach)

```bash
az aks create -g phase7 -n aks-phase7   --node-count 1   --enable-managed-identity   --network-plugin azure   --vnet-subnet-id $(az network vnet subnet show -g phase7 --vnet-name vnet-phase7 -n aks-subnet --query id -o tsv)   --attach-acr phase7registry

az aks get-credentials -g phase7 -n aks-phase7
```

---

### 5️⃣ Enable Application Gateway Ingress Controller (AGIC)

```bash
APPGW_ID=$(az network application-gateway show -g phase7 -n appgw-phase7 --query id -o tsv)

az aks enable-addons -g phase7 -n aks-phase7   -a ingress-appgw --appgw-id $APPGW_ID

kubectl get pods -n kube-system -l app=ingress-appgw -o wide
```

---

### 6️⃣ Config Server Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: config-server
  labels: { app: config-server }
spec:
  replicas: 1
  selector: { matchLabels: { app: config-server } }
  template:
    metadata: { labels: { app: config-server } }
    spec:
      containers:
        - name: config-server
          image: phase7registry.azurecr.io/cloud-java-configserver:no-eureka-3
          ports: [ { containerPort: 8888 } ]
---
apiVersion: v1
kind: Service
metadata:
  name: config-server
spec:
  type: ClusterIP
  selector: { app: config-server }
  ports:
    - port: 8888
      targetPort: 8888
```

```bash
kubectl apply -f deploy-config-server.yaml
kubectl get svc config-server
```

---

### 7️⃣ Tasks Service Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: tasks-service
  labels: { app: tasks-service }
spec:
  replicas: 1
  selector:
    matchLabels: { app: tasks-service }
  template:
    metadata:
      labels: { app: tasks-service }
    spec:
      containers:
      - name: tasks-service
        image: phase7registry.azurecr.io/cloud-java-tasksservice:no-eureka-3
        ports: [ { containerPort: 8081 } ]
        env:
        - { name: SPRING_PROFILES_ACTIVE, value: "docker" }
        - { name: SPRING_CLOUD_CONFIG_URI, value: "http://config-server.default.svc.cluster.local:8888" }
        - { name: SPRING_DATASOURCE_URL, value: "jdbc:postgresql://pgtaskphase7.postgres.database.azure.com:5432/tasksbd?sslmode=require" }
        - { name: SPRING_DATASOURCE_USERNAME, value: "phase7admin" }
        - { name: SPRING_DATASOURCE_PASSWORD, value: "Phase7!Pass123" }
        startupProbe:
          httpGet: { path: /actuator/health, port: 8081 }
          initialDelaySeconds: 60
          periodSeconds: 10
          failureThreshold: 30
        readinessProbe:
          httpGet: { path: /actuator/health, port: 8081 }
          initialDelaySeconds: 30
          periodSeconds: 10
          failureThreshold: 12
        livenessProbe:
          httpGet: { path: /actuator/health, port: 8081 }
          initialDelaySeconds: 120
          periodSeconds: 20
          failureThreshold: 6
        resources:
          requests: { cpu: "50m", memory: "256Mi" }
          limits:   { cpu: "500m", memory: "1Gi" }
---
apiVersion: v1
kind: Service
metadata:
  name: tasks-service
spec:
  type: ClusterIP
  selector: { app: tasks-service }
  ports:
    - name: http
      port: 8081
      targetPort: 8081
```

---

### 8️⃣ Ingress (AGIC)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tasks-ingress
  annotations:
    appgw.ingress.kubernetes.io/health-probe-path: /actuator/health
spec:
  ingressClassName: azure-application-gateway
  rules:
  - http:
      paths:
      - path: /api/tasks
        pathType: Prefix
        backend:
          service:
            name: tasks-service
            port: { number: 8081 }
      - path: /actuator/health
        pathType: Prefix
        backend:
          service:
            name: tasks-service
            port: { number: 8081 }
```

---

### 9️⃣ Test & Logs

```bash
APPGW=$(az network public-ip show -g phase7 -n appw-phase7-ip --query ipAddress -o tsv)

# External tests
curl -I http://$APPGW/actuator/health
curl -i http://$APPGW/api/tasks

# Internal tests
kubectl run curl --rm -it --restart=Never --image=curlimages/curl:8.10.1 -- \
  curl -sSI http://tasks-service.default.svc.cluster.local:8081/actuator/health

kubectl run curl --rm -it --restart=Never --image=curlimages/curl:8.10.1 -- \
  curl -i http://tasks-service.default.svc.cluster.local:8081/api/tasks

# Logs
kubectl logs deploy/tasks-service --tail=200 | grep -iE "ERROR|Exception"
```

---

### 🔍 Notes

- Database: `tasksbd` on `pgtaskphase7` (SSL required)
- Probes: `/actuator/health` for all
- Network plugin: **Azure CNI**
- Ingress class: `azure-application-gateway`
- Images built for **amd64 + arm64**
- Config Server shared by services

---

### 🧹 Cleanup

```bash
az group delete -n phase7 --yes --no-wait
```

---

### 📦 Tag this release

```bash
git add README.md
git commit -m "docs: Phase 7 AKS deployment guide"
git tag phase-7
git push origin main --tags
```
