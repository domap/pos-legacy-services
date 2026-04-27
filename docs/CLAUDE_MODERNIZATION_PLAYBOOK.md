# Claude modernization playbook — agents, steps, observability

Use this with **Claude Projects** (project knowledge: attach `docs/DEEPWIKI.md` + this file) or as **session instructions** for each chat. It defines **parent agents**, **sub-agents**, a **step-by-step pipeline**, and **mandatory observability** for every service.

**GitHub Copilot:** Repo instructions in `.github/copilot-instructions.md`. **Custom agents** (embedded prompts + handoff workflow) in **`.github/agents/*.agent.md`** — see **`docs/COPILOT_CLAUDE_MODES.md`**.
For no-manual workflow in Copilot, select **`p0-orchestrator`** only; it invokes specialized sub-agents automatically.

**Twelve-factor checklist (explicit):** `docs/TWELVE_FACTOR.md` — maps all 12 factors to this repo; sub-agent **S4** implements config/backing-service aspects in code.

**Multi-brand strategy:** `docs/MULTI_BRAND_ARCHITECTURE.md` — canonical model boundaries, provider adapters, and per-brand configuration/routing.

---

## Principles for every Claude session

1. **Read first:** Open `docs/DEEPWIKI.md` for the service under work. Do not invent endpoints or ports.
2. **One PR scope per sub-agent:** Small, reviewable changes with tests.
3. **Observability is not optional:** No sub-agent closes work without the observability checklist for that slice (below).
4. **Backward compatibility:** Unless explicitly versioning (`/api/v2`), preserve paths and query parameter names from Deepwiki.
5. **Modernization minimum bar per service:** Spring Boot + Java upgrade, 12-factor config externalization, containerization, ECR image publishing path, AWS Secrets Manager integration, and EKS deployability.

---

## Observability baseline (all four services)

Every modernized service must include:

| Layer | Requirement |
|-------|-------------|
| **Logs** | SLF4J + Logback (or Log4j2) with **JSON** encoding in prod; include `traceId`, `spanId` (MDC) when OTel is present |
| **Traces** | **OpenTelemetry** Java agent or SDK; export to OTLP (collector / ADOT / vendor) |
| **Metrics** | **Micrometer** with Prometheus registry or OTLP metrics; minimum **HTTP server** + **JVM** + **custom** outbound client timers |
| **Health** | Spring Boot **Actuator**: `health`, **`readiness`** and **`liveness`** groups; readiness must reflect DB or critical upstream config |
| **Correlations** | `X-Request-Id` or W3C **traceparent** propagation on inbound and outbound HTTP |

### Standard dependencies (reference — adjust versions to your Spring Boot / JDK)

- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus` (if scraping Prometheus)
- OpenTelemetry: BOM + `opentelemetry-spring-boot-starter` or Java agent in container **ENTRYPOINT**
- Optional: `spring-boot-starter-aop` for timed aspects on clients

### Standard Actuator exposure (conceptual)

- Expose `health`, `info`, `metrics`, `prometheus` on a **management port** or path prefix `/actuator` secured in prod.
- Kubernetes: **liveness** ≠ **readiness** (DB/upstream for customer; config for others).

### Sub-agent: Observability Lead (run once per service after skeleton exists)

**Inputs:** Service name, `application.yml` structure, list of outbound clients.  
**Outputs:** PR adding actuator, OTel wiring, log JSON pattern, dashboard JSON or CloudWatch metric filters (as text artifacts if no infra repo yet).  
**Done when:** Local run shows `/actuator/health/readiness` 200; a test request creates a trace span (verify in collector logs or console exporter in dev).

---

## Agent roster

### Parent agents (orchestration — long-lived or human-led)

| ID | Name | Responsibility |
|----|------|------------------|
| **P0** | **Program Orchestrator** | Sequencing phases, acceptance criteria, conflict resolution between services |
| **P1** | **Platform / EKS** | Docker, Helm, CI, secrets operator, IRSA — not business logic |
| **P2** | **Java Platform** | JDK version, parent POM, Spring Boot upgrade, shared BOM |
| **P3** | **Architecture & API** | URL compatibility, DTO vs map decisions, versioning |
| **P4** | **Security & Compliance** | Secrets, scanning, SBOM, non-root containers |

### Sub-agents (single-focus — one Claude chat each)

| ID | Name | Typical deliverable |
|----|------|----------------------|
| **S1** | **Inventory & Test Harness** | Contract tests from Deepwiki; WireMock for Kobie/SFCC/Vertex |
| **S2** | **Build & Module Shape** | JAR packaging, `Dockerfile`, `mvnw` CI job |
| **S3** | **Spring Boot Upgrade** | 2.7 → 3.x+, `javax` → `jakarta`, dependency fixes |
| **S4** | **Config & 12-factor** | `@ConfigurationProperties`, env var mapping, no secrets in repo |
| **S5** | **Persistence** (customer only) | Flyway, RDS URL, H2 test profile, readiness = DB |
| **S6** | **HTTP Clients** | RestClient/WebClient, timeouts, metrics, tracing on outbound calls |
| **S7** | **Observability** | Actuator + Micrometer + OTel + log JSON (per baseline above) |
| **S8** | **Kubernetes** | Deployment, Service, probes, resources, ServiceAccount + IRSA annotations |
| **S9** | **Docs & Deepwiki** | Update `DEEPWIKI.md` when behavior changes |

---

## Step-by-step pipeline (with agents)

Run phases in order; **parallelize** only where noted.

### Phase 0 — Program kickoff

**Agent:** P0 Program Orchestrator  

**Steps:**

1. Confirm target JDK (e.g. 25) and Spring Boot line that supports it.
2. Attach **`docs/DEEPWIKI.md`** to the Claude project (or paste summary + link).
3. Define “done”: e.g. all services JAR + image + Helm + observability + CI green.

**Output:** A short **migration board** (table: service × phase status).

---

### Phase 1 — Shared Java platform (once)

**Agent:** P2 Java Platform  

**Sub-agents (sequential):**

1. **S1** — Add **integration tests** or **contract tests** that hit current embedded apps (record golden JSON for each endpoint in Deepwiki).
2. **S2** — Introduce optional **parent POM** or documented shared versions in root `README` (if mono-repo build is desired).

**Observability:** Not required yet; tests are the gate.

---

### Phase 2 — Per service vertical slice (repeat ×4)

Order recommendation: **customer-lookup** (hardest: DB) → **tax-vertex** → **loyalty-kobie** → **products-sfcc** (or parallel tax + loyalty after customer reference is done).

For **each** service:

| Step | Sub-agent | Actions |
|------|-----------|---------|
| 2a | **S3** Spring Boot Upgrade | Bump parent; fix `javax`/`jakarta`; make app start |
| 2b | **S4** Config | Externalize URLs/tokens; profiles `dev` / `test` / `prod` |
| 2c | **S5** (customer only) | RDS-ready config; Flyway; H2 limited to `test` |
| 2d | **S6** HTTP Clients | Replace raw RestTemplate patterns; connect/read timeouts; metrics |
| 2e | **S7** Observability | Actuator + Micrometer + OTel + structured logs (**mandatory**) |
| 2f | **S2** Container | Multi-stage `Dockerfile`, non-root, `JAVA_TOOL_OPTIONS` for agent if used |
| 2g | **S8** K8s | Helm chart values: probes hit readiness path; resources; IRSA placeholder |
| 2h | **S9** Docs | Update `DEEPWIKI.md` for any intentional API or config change |

**Gate before next service:** CI passes; **readiness** probe green locally or in dev cluster; **one** golden trace visible in dev collector.

---

### Phase 3 — Platform hardening

**Agent:** P1 Platform / EKS + **P4** Security  

**Sub-agents:**

- **S8** — Wire **External Secrets** (or CSI) to chart; remove plaintext secrets from values.
- **P4** — Image scan policy, `readOnlyRootFilesystem`, drop capabilities.

---

### Phase 4 — Release engineering

**Agent:** P0 + P1  

**Steps:**

1. Tag strategy; semver per service.
2. Deploy to **EKS** dev; smoke tests per Deepwiki endpoints.
3. Progressive rollout (optional): canary / Argo Rollouts.

---

## Copy-paste: Claude system prompts (concise)

Use as **custom instructions** or first message in a dedicated chat.

### Program Orchestrator (P0)

```text
You are the Program Orchestrator for modernizing the pos-legacy Spring Boot services.
Rules: (1) Always consult docs/DEEPWIKI.md for current APIs and ports. (2) Enforce observability: Actuator health/readiness/liveness, Micrometer metrics, OpenTelemetry traces, JSON logs with trace correlation. (3) One PR-sized change set per recommendation. (4) Preserve public HTTP contracts unless versioning is explicit.
Output: numbered plan, risks, and which sub-agent should execute next.
```

### Spring Boot upgrade (S3)

```text
You are the Spring Boot upgrade sub-agent. Input: one service module path. Upgrade Spring Boot and migrate javax→jakarta where required. Do not change HTTP routes. Run/fix tests. Do not add observability yet unless blocking compilation.
```

### Observability (S7)

```text
You are the Observability sub-agent. For the given service: add spring-boot-starter-actuator with separate readiness vs liveness; Micrometer for HTTP and outbound clients; OpenTelemetry (SDK or document agent); JSON logging with traceId/spanId in MDC. Expose prometheus or OTLP per Spring Boot best practices. Update Kubernetes probe paths in comments or Helm snippets.
```

### Kubernetes (S8)

```text
You are the K8s packaging sub-agent. Produce Helm values and manifests for EKS: Deployment, Service, Ingress annotations for AWS LB Controller, ServiceAccount for IRSA, resource requests/limits, probes to actuator readiness/liveness, PodDisruptionBudget optional. No secrets in Git — use ExternalSecrets placeholders.
```

---

## Handoff template (between chats)

Paste when spawning a new sub-agent:

```text
Service: <name>
Repo path: <module>
Prior work: <link/branch/summary>
Must read: docs/DEEPWIKI.md § <section>
This task: <S7 etc.>
Constraints: preserve GET/POST paths and query names from Deepwiki; add observability per CLAUDE_MODERNIZATION_PLAYBOOK.md
Deliverable: <files> + how to verify locally
```

---

## Verification checklist (before marking a service “modern”)

- [ ] All Deepwiki endpoints return compatible shapes (automated golden tests).
- [ ] No `System.out.println` in production paths (or guarded / removed).
- [ ] `/actuator/health/readiness` reflects real dependencies.
- [ ] Metrics show `http.server.requests` and outbound client duration.
- [ ] Trace spans: inbound + outbound (where applicable).
- [ ] `docker run` + Helm dry-run documented in module `README` or root doc.

---

## Related files

- **`docs/DEEPWIKI.md`** — Current behavior reference for code generation.
- Per-module **`DEPLOY.txt`** — Legacy Tomcat notes (replace with container runbooks over time).
