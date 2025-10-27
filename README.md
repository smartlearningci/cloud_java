# ☁️ Cloud Java — Phase 6 (Azure Deployment)

## 📘 Context

In this phase, the **Cloud Java** project transitions from a **local infrastructure (Phase 5)** to a **fully managed infrastructure in Microsoft Azure**.

The goal is to reproduce the same microservices ecosystem (`config-server`, `discovery`, `tasks-service`, and `gateway`), now with:
- **Azure Container Apps** instead of local Docker containers;
- **Azure Container Registry (ACR)** as the image repository;
- **PostgreSQL Flexible Server** as a managed database service;
- **Log Analytics** and **Managed Environment** for integrated monitoring.

This results in a lightweight, scalable production environment with automatic networking and logging management — maintaining the same functional architecture used locally.

---

## ⚙️ Prerequisites

Before running this script, make sure you have:

1. **An active Azure account** with permission to create resources.
2. **Azure CLI** (v2.58 or later) with the following extensions installed:
   ```bash
   az extension add --name containerapp
   az extension add --name log-analytics
   ```
3. **Docker Desktop** installed and authenticated.
4. Logged in to Azure CLI with the correct subscription:
   ```bash
   az login
   az account set --subscription "<SUBSCRIPTION_NAME_OR_ID>"
   ```

---

## 🧱 Code Structure

The repository should contain the following directories:

```
./config-server
./discovery
./tasks-service
./gateway
```

Each directory contains its respective `Dockerfile` for building and pushing images.

---

## 🚀 Phase 6 Objective

> Recreate the entire "cloud-java" environment in **Azure**, replacing the local Docker environment with a solution based on **Azure Container Apps** and **Azure Database for PostgreSQL Flexible Server**, with centralized monitoring through **Log Analytics**.

---

## 🪄 Main Steps

### 1️⃣ Register Azure Resource Providers

```bash
for NS in Microsoft.ContainerRegistry Microsoft.App Microsoft.OperationalInsights Microsoft.DBforPostgreSQL; do
  az provider register --namespace $NS
done

echo "Waiting for providers to be registered..."
for NS in Microsoft.ContainerRegistry Microsoft.App Microsoft.OperationalInsights Microsoft.DBforPostgreSQL; do
  while true; do
    STATE=$(az provider show --namespace $NS --query "registrationState" -o tsv)
    echo "$NS => $STATE"
    [ "$STATE" = "Registered" ] && break
    sleep 5
  done
done
```

Create the **Resource Group**, **Log Analytics Workspace**, and **Container Apps Environment**:

```bash
az group create --name phase6 --location northeurope

az monitor log-analytics workspace create   --resource-group phase6   --workspace-name workspace-phase6   --location northeurope

az containerapp env create   --name managedEnvironment-phase6   --resource-group phase6   --location northeurope   --logs-workspace-id "$(az monitor log-analytics workspace show -g phase6 -n workspace-phase6 --query customerId -o tsv)"   --logs-workspace-key "$(az monitor log-analytics workspace get-shared-keys -g phase6 -n workspace-phase6 --query primarySharedKey -o tsv)"
```

---

### 2️⃣ Create Azure Container Registry (ACR)

```bash
az acr check-name --name phase6acr -o table
az acr create --resource-group phase6 --name phase6acr --sku Standard --admin-enabled true
az acr login --name phase6acr

az acr show -n phase6acr --query "{loginServer:loginServer, sku:sku.name, admin:adminUserEnabled}" -o table
az acr check-health -n phase6acr --yes
```

---

### 3️⃣ Build and Push Docker Images

```bash
# CONFIG SERVER
docker build -f ./config-server/Dockerfile -t phase6acr.azurecr.io/cloud-java-configserver:latest .
docker push phase6acr.azurecr.io/cloud-java-configserver:latest

# DISCOVERY
docker build -f ./discovery/Dockerfile -t phase6acr.azurecr.io/cloud-java-discovery:latest .
docker push phase6acr.azurecr.io/cloud-java-discovery:latest

# TASKS SERVICE
docker build -f ./tasks-service/Dockerfile -t phase6acr.azurecr.io/cloud-java-tasksservice:latest .
docker push phase6acr.azurecr.io/cloud-java-tasksservice:latest

# GATEWAY
docker build -f ./gateway/Dockerfile -t phase6acr.azurecr.io/cloud-java-gateway:latest .
docker push phase6acr.azurecr.io/cloud-java-gateway:latest
```

---

### 4️⃣ PostgreSQL Flexible Server (Low Cost, North Europe)

```bash
az postgres flexible-server create   --resource-group phase6   --name pgtasksphase6   --location northeurope   --tier Burstable --sku-name standard_b1ms --storage-size 32   --version 16   --zone 1   --backup-retention 7   --geo-redundant-backup Disabled   --storage-auto-grow Disabled   --public-access 0.0.0.0-255.255.255.255   --admin-user phase6admin   --admin-password 'Phase6!Pass123'

az postgres flexible-server db create   --resource-group phase6   --server-name pgtasksphase6   --database-name tasksdb
```

**Connection details:**

| Parameter | Value |
|------------|--------|
| Host | `pgtasksphase6.postgres.database.azure.com` |
| User | `phase6admin@pgtasksphase6` |
| Password | `Phase6!Pass123` |
| DB | `tasksdb` |
| JDBC | `jdbc:postgresql://pgtasksphase6.postgres.database.azure.com:5432/tasksdb?sslmode=require` |

---

### 5️⃣ Deploy Container Apps

(Commands for `config-server`, `discovery`, `tasks-service`, and `gateway` are identical to previous sections in this repository — adapted for Azure deployment.)

---

### 6️⃣ Obtain Gateway FQDN and Test

```bash
az containerapp show -g phase6 -n gateway --query "properties.configuration.ingress.fqdn" -o tsv
curl -sSI https://<GATEWAY_FQDN>/api/tasks
```

---

## 🔍 Diagnostics and Logs

```bash
# Check registered apps in Eureka
az containerapp exec -g phase6 -n gateway --command 'sh -lc "wget -q -O - http://discovery.internal.$(az containerapp env show -g phase6 -n managedEnvironment-phase6 --query properties.defaultDomain -o tsv)/eureka/apps | head -n 80"'

# Verify configuration fetch
az containerapp exec -g phase6 -n gateway --command 'sh -lc "wget -S -O - http://config-server.internal.$(az containerapp env show -g phase6 -n managedEnvironment-phase6 --query properties.defaultDomain -o tsv)/tasks-service/docker 2>&1 | head -n 20"'

# Show logs
az containerapp logs show -g phase6 -n tasks-service --tail 200
```

---

## 📦 Final Result

After completing Phase 6, the system runs entirely on **Azure Cloud**, featuring:
- Fully managed infrastructure (no manual VMs);
- Modular deployments using Container Apps;
- Managed PostgreSQL database;
- Centralized monitoring with Log Analytics.

---

## 🏷️ Git Tag

```bash
git tag -a phase-6 -m "Full Azure deployment — migrated from local infrastructure"
git push origin phase-6
```

---

**Authors:**  
Smart Learning — *Cloud Java Infrastructure*  
Phase 6 — *Azure Container Apps Deployment*
