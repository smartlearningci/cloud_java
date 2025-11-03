# 🧩 PHASE 8 – Automated CI/CD (No Command Line)

## 🧠 1. Core Concepts

### 🔹 What is CI/CD and why use it
- **Continuous Integration (CI)** ensures that every commit is automatically built, validated, and packaged.  
- **Continuous Delivery / Deployment (CD)** automates the delivery of new versions to production environments.  
- The goal is to eliminate manual steps (`docker build`, `docker push`, `kubectl apply`), reduce human error, and ensure repeatable, traceable releases.

```
   +------------+        +------------------+        +----------------+
   |  Developer |  Push  | GitHub Workflow  | Deploy | Azure (AKS)   |
   |  Commit    | -----> |  (CI/CD Action)  | -----> | Pods Updated  |
   +------------+        +------------------+        +----------------+
```

### 🔐 OpenID Connect (OIDC)
OIDC allows GitHub Actions to log in to Azure securely **without secrets or passwords**.  
Azure Entra ID trusts GitHub as an identity provider and issues short-lived access tokens.

```
GitHub Action  ---- OIDC Token ---->  Azure Entra ID  ---->  Authorized Access
```

### 🏗️ Azure Container Registry (ACR)
The ACR stores Docker images built by GitHub Actions.

```
+-----------+     +----------------+     +---------------+
|  Source   | --> |  Build Docker  | --> |  Push to ACR  |
+-----------+     +----------------+     +---------------+
```

### ☸️ Azure Kubernetes Service (AKS)
AKS pulls the new image and performs a **rolling update** to replace old pods with new ones.

```
[v1 pod] [v1 pod]  →  [v2 pod] [v2 pod]
(remove old)         (create new gradually)
```

---

## ⚙️ 2. Practical Implementation

### ✅ Prerequisites (from Phase 7)
- Azure Kubernetes Service (AKS) running `tasks-service` and `config-server`
- Azure Container Registry (ACR) with Docker images
- Kubernetes Deployment YAMLs for both services
- GitHub repository `smartlearningci/cloud_java` with latest code

```
+--------------------+            +----------------------+
|   Developer (STS)  |            |  GitHub Repository   |
|  code changes ---> |  Commit →  |  cloud_java          |
+--------------------+            +----------------------+
                                        |
                                        v
                                +-------------------+
                                | GitHub Actions CI |
                                +-------------------+
                                        |
                                        v
+---------------------+          +-------------------------+
| Azure Container     | Push     | Azure Kubernetes Service |
| Registry (ACR)      |--------> | (AKS) updates pods       |
+---------------------+          +-------------------------+
```

---

## 1️⃣ Create a Federated Identity (OIDC) in Azure Portal

**Goal:** allow GitHub to authenticate to Azure securely without storing credentials.

**Steps in Azure Portal**
1. Go to **Microsoft Entra ID → App registrations → New registration**
   - **Name:** `gh-oidc-cloud-java`
   - **Supported account type:** Single tenant
   - **Redirect URI:** leave blank
   - Click **Register**
2. After creation, note:
   - **Application (Client) ID**
   - **Directory (Tenant) ID**
3. Open **Certificates & Secrets → Federated credentials → + Add credential**
   - Type: **GitHub Actions**
   - Organization: `smartlearningci`
   - Repository: `cloud_java`
   - Branch: `phase-8` (or `main`)
   - Entity type: **Branch**
   - Save

```
GitHub Actions  ─────────►  Azure Entra ID (OIDC)
       │                        │
       │   issues secure token  │
       └────────────────────────┘
```

---

## 2️⃣ Assign Permissions (RBAC)

The application `gh-oidc-cloud-java` needs access to Azure resources.

### a) Subscription
- Role → **Reader**
- Resource → Subscription
- Assign to → `gh-oidc-cloud-java`

### b) ACR
- Role → **AcrPush**
- Resource → Container Registry (`phase7registry`)
- Assign to → `gh-oidc-cloud-java`

```
GitHub Actions
    │
    ▼
[ ACR ]
 | receives docker push
 | stores image
 +----------------------+
```

### c) AKS
- Role → **Azure Kubernetes Service RBAC Cluster Admin**
- Resource → AKS Cluster (`aks-phase7`)
- Assign to → `gh-oidc-cloud-java`

```
GitHub Actions
    │
    ▼
[ AKS Cluster ]
 | kubectl set image
 | rollout update
 +------------------+
```

> ⏳ Wait ~1–3 minutes for RBAC propagation before running the workflow.

---

## 3️⃣ Create GitHub Secrets (with exact Azure Portal locations)

In GitHub → **Settings → Secrets and variables → Actions → New repository secret**.  
Create these three secrets, with values taken from the Azure Portal at the exact locations below:

| Secret Name | Where to find in Azure Portal | Value to copy |
|---|---|---|
| `AZURE_CLIENT_ID` | **Microsoft Entra ID → App registrations →** select `gh-oidc-cloud-java` → **Overview** | **Application (client) ID** |
| `AZURE_TENANT_ID` | **Microsoft Entra ID → App registrations →** select `gh-oidc-cloud-java` → **Overview** | **Directory (tenant) ID** |
| `AZURE_SUBSCRIPTION_ID` | **Subscriptions →** select your active subscription → **Overview** | **Subscription ID** |

```
+-----------------------------------+
| GitHub Secrets (encrypted)        |
| - AZURE_CLIENT_ID                 |
| - AZURE_TENANT_ID                 |
| - AZURE_SUBSCRIPTION_ID           |
+-----------------------------------+
```

---

## 4️⃣ Configure CI/CD from AKS Portal

1. Open your AKS resource in the Azure Portal  
2. Navigate to **Deployment Center** → **Configure CI/CD (GitHub Actions)**  
3. Log in with your GitHub account  
4. Select:  

| Field | Value |
|-------|-------|
| Organization | smartlearningci |
| Repository | cloud_java |
| Branch | phase-8 (or main) |
| Docker file | `tasks-service/Dockerfile` |
| Image name | `cloud-java-tasksservice` |
| Container registry | `phase7registry` |
| Kubernetes manifest | YAML for `tasks-service` |
| Deployment strategy | kubectl |

The portal will:
- auto-create `.github/workflows/azure-kubernetes-service.yml`
- open a Pull Request
- once merged, activate the pipeline

```
.github/
 └── workflows/
      └── azure-kubernetes-service.yml
tasks-service/
 ├── Dockerfile
 ├── pom.xml
 ├── src/
 └── deployment.yml
```

---

## 5️⃣ Final Workflow YAML

High-level steps:
```
GitHub Actions CI/CD
 ├── Build (Maven)
 ├── Docker build & push → ACR
 ├── AKS login & rollout
 └── Check pods
```

```yaml
name: CI/CD tasks-service (AKS)

on:
  push:
    branches: [ phase-8, main ]
    paths: [ "tasks-service/**" ]

permissions:
  id-token: write
  contents: read

env:
  RG: phase7
  AKS: aks-phase7
  ACR_NAME: phase7registry
  ACR_LOGIN: phase7registry.azurecr.io
  NS: default
  DEPLOYMENT_NAME: tasks-service
  CONTAINER_NAME: tasks-service
  IMAGE_NAME: cloud-java-tasksservice
  ROLLOUT_TIMEOUT: 10m

jobs:
  build-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
      - run: mvn -B -DskipTests -f tasks-service/pom.xml package

      - uses: azure/login@v2
        with:
          client-id: ${{ secrets.AZURE_CLIENT_ID }}
          tenant-id: ${{ secrets.AZURE_TENANT_ID }}
          subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}

      - name: ACR login
        run: |
          TOKEN=$(az acr login --name $ACR_NAME --expose-token --query accessToken -o tsv)
          echo "$TOKEN" | docker login $ACR_LOGIN -u 00000000-0000-0000-0000-000000000000 --password-stdin

      - name: Build & Push
        run: |
          TAG=${{ github.sha }}
          docker build -t $ACR_LOGIN/$IMAGE_NAME:$TAG -t $ACR_LOGIN/$IMAGE_NAME:latest -f tasks-service/Dockerfile .
          docker push $ACR_LOGIN/$IMAGE_NAME:$TAG
          docker push $ACR_LOGIN/$IMAGE_NAME:latest
          echo "IMAGE=$ACR_LOGIN/$IMAGE_NAME:$TAG" >> $GITHUB_ENV

      - uses: azure/aks-set-context@v4
        with:
          resource-group: ${{ env.RG }}
          cluster-name: ${{ env.AKS }}

      - name: Rollout update
        run: |
          kubectl -n $NS set image deployment/$DEPLOYMENT_NAME $CONTAINER_NAME=${IMAGE}
          kubectl -n $NS rollout status deployment/$DEPLOYMENT_NAME --timeout=${{ env.ROLLOUT_TIMEOUT }}
```

---

## 6️⃣ Full Visual Flow
```
┌────────────────────────────┐
│   1. Commit in GitHub      │
│   (branch phase-8 or main) │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│  2. GitHub Action starts   │
│  - OIDC login to Azure     │
│  - Maven build             │
│  - docker build/push       │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│  3. ACR receives new image │
│     cloud-java-tasksservice│
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│  4. AKS updates deployment │
│     kubectl set image      │
│     rollout status         │
└─────────────┬──────────────┘
              │
              ▼
┌────────────────────────────┐
│  5. New Pod is Ready       │
│     with new image         │
└────────────────────────────┘
```

---

## 7️⃣ Hands-on Demo (Visual)

- Edit something visible in `TaskController` (e.g., “Phase 8 – CI/CD Active”).
- Commit + push to `phase-8`.
- In GitHub → **Actions**, watch: **build → push → deploy**.

In Azure Portal:
- **AKS → Workloads → Deployments → tasks-service**
  - Observe pods restarting automatically.
- Open the public endpoint and verify the change.

**Pipeline:**  
`Commit → Build → Push → Deploy → New Pod → Updated App`

---

## ⚠️ Runtime Notes (Timeout Case)

If the workflow fails with:
```
error: timed out waiting for the condition
```
✅ Check AKS: pods likely took longer than the default timeout. The **deploy actually succeeded**, only the **status wait timed out**.

**Solution:** extend the wait in the workflow:
```yaml
kubectl -n $NS rollout status deployment/$DEPLOYMENT_NAME --timeout=10m
```

---

**Repo:** `smartlearningci/cloud_java`  
**Phase:** `phase-8`  
**Monitoring:** reserved for **Phase 9** (Grafana/Prometheus).
