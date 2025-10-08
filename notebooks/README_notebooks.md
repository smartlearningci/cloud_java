# 📚 Notebooks — TaskFlow (Formação Cloud Native)

Este diretório contém os notebooks utilizados ao longo da formação. A narrativa é **evolutiva**: cada fase **adiciona uma camada** sem apagar a anterior.

> Repositório: `smartlearningci/cloud_java`  
> Pasta sugerida: `notebooks/`

---

## Índice

1. **01 — Conceitos & Arquitetura**  
   _Cloud Native, benefícios, padrões (Gateway, Config, Discovery, Resilience, Observabilidade) e diagramas das Fases 0→4._  
   **Abrir no Colab:** <https://colab.research.google.com/github/smartlearningci/cloud_java/blob/main/notebooks/01_TaskFlow_Conceitos_Arquitetura.ipynb>

2. **02 — API MVP (tasks-service)**  
   _Domínio `Task`, contratos REST, snippets em Java (Entity/Repository/Controller) e testes por `curl`._  
   **Abrir no Colab:** <https://colab.research.google.com/github/smartlearningci/cloud_java/blob/main/notebooks/02_TaskFlow_API_MVP.ipynb>

3. **03 — API Gateway & Perfis**  
   _Spring Cloud Gateway (artefacto não deprecado), chaves de configuração novas e variável `TASKS_BASE_URL` com `fallback`._  
   **Abrir no Colab:** <https://colab.research.google.com/github/smartlearningci/cloud_java/blob/main/notebooks/03_TaskFlow_Gateway_Roteamento_Perfis.ipynb>

4. **04 — Docker Compose & Troubleshooting**  
   _Compose de `gateway` + `tasks-service`, healthchecks, `run_compose.sh` e resolução de erros reais (UnknownHost, manifest, 404, MVC/WebFlux)._  
   **Abrir no Colab:** <https://colab.research.google.com/github/smartlearningci/cloud_java/blob/main/notebooks/04_TaskFlow_Docker_Compose_Troubleshooting.ipynb>

> ⚠️ Se a `branch` ou o caminho forem diferentes, ajusta o URL do Colab:  
> `https://colab.research.google.com/github/<ORG>/<REPO>/blob/<BRANCH>/notebooks/<FICHEIRO>.ipynb`

---

## Requisitos locais para executar os exemplos

- **Java 21** (JDK)  
- **Maven**  
- **Docker** e **Docker Compose** (para a execução em contentores)

---

## Comandos úteis (resumo)

### Correr local (sem Docker)
1) Arranca o **tasks-service** (porta `8081`) no STS/IDE.  
2) Arranca o **gateway** (porta `8080`) no STS/IDE.  
3) Testes:
```bash
# via gateway
curl -s http://localhost:8080/api/tasks | jq
# direto
curl -s http://localhost:8081/tasks | jq
```

> Se quiseres parametrizar a URL do backend no Gateway: define `TASKS_BASE_URL=http://localhost:8081`.  
> Sem variável, o gateway faz `fallback` para `http://localhost:8081`.

### Correr com Docker Compose
```bash
./run_compose.sh
# ou
docker compose up --build
```
Testes:
```bash
curl -s http://localhost:8080/api/tasks | jq
curl -s http://localhost:8081/tasks | jq
```

> No Compose, o Gateway deve receber `TASKS_BASE_URL=http://tasks-service:8081` via `environment`.

---

## Troubleshooting rápido

- **UnknownHost `tasks-service`** ao correr fora do Docker  
  ➜ definir `TASKS_BASE_URL=http://localhost:8081` ou usar perfis locais.

- **`no main manifest attribute`** no container do gateway  
  ➜ garantir `spring-boot-maven-plugin` com `repackage` e copiar o jar **sem** `.original` no `Dockerfile`.

- **404 Whitelabel** ao chamar `/api/tasks` no `8081`  
  ➜ no backend é `/tasks`; `/api/tasks` é via Gateway em `8080`.

- **Conflito MVC vs WebFlux**  
  ➜ remover dependências do Gateway do `tasks-service`; forçar `spring.main.web-application-type=servlet` se necessário.

---

Bom estudo e bons deploys! 🚀
