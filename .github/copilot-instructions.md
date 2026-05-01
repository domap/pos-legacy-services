# IDE assistant — legacy POS monorepo modernization

This monorepo has **four** Spring Boot HTTP modules. **Post-migration:** every service is an **executable JAR** in an **OCI image** — **no WAR** on the release path (`docs/DEEPWIKI.md`). **Today:** **three** modules are still **Spring Boot 2.7.x / Java 8** with interim **WAR** packaging (`spring-boot-starter-tomcat` **provided**); **`customer-lookup-legacy-service`** already matches the target (**Java 25**, **Spring Boot 3.5.x**, **Jakarta**, **JAR**, **Flyway**, **Actuator**, observability hooks, **Helm / CI**). The program still drives **JDK alignment**, **12-factor** config, **Kubernetes** deployability, and full **observability** per module.

## Always follow

1. **Source of truth for APIs and ports:** `docs/DEEPWIKI.md` — do not invent routes or response shapes.
2. **Twelve-factor:** `docs/TWELVE_FACTOR.md` — config via environment; no secrets in committed files.
3. **Orchestration & observability baseline:** `docs/CLAUDE_MODERNIZATION_PLAYBOOK.md` — Actuator readiness/liveness, Micrometer, OpenTelemetry, JSON logs.
4. **Multi-brand architecture:** `docs/MULTI_BRAND_ARCHITECTURE.md` — canonical models + provider adapters + brand-aware configuration.
5. **Scope:** Prefer one logical change set per task (one sub-agent scope). Preserve public HTTP contracts unless versioning is explicit (`/api/v2`).
6. **Minimum modernization gates per service:** JDK/Spring upgrade (per policy), 12-factor config, containerization, image publishing to your **registry** (e.g. **ECR**), **secret store** integration (e.g. **AWS Secrets Manager** + External Secrets), and **Kubernetes** (e.g. **EKS**) deployment readiness.
7. **OpenRewrite usage:** Recommended for mechanical upgrade refactors (`javax` -> `jakarta`, Spring migrations); always review generated diffs and validate with tests.

## Custom agents (workflow — no prompt copy-paste)

Specialist agents with **embedded system prompts** live in **`.github/agents/*.agent.md`**. In your IDE’s agent chat (e.g. **Copilot Chat**), **select the agent from the dropdown**; use **handoff buttons** where the product supports them (VS Code) to move to the next step.

**How and when:** **`docs/COPILOT_CLAUDE_MODES.md`**

### Single-agent workflow

- Use **`p0-orchestrator`** as the only entrypoint.
- `p0-orchestrator` invokes all required sub-agents (`S1..S9`, `P1..P4`) as needed.
- Other agents are hidden from direct selection (`user-invocable: false`) and exist for orchestration.
- Each run is **independent per service**: developer must select one target module and request modernization for that module only.

## Services (modules)

- `customer-lookup-legacy-service` — JPA + Flyway + Postgres in prod / H2 in dev & tests; Actuator + metrics (see `docs/DEEPWIKI.md`)
- `loyalty-kobie-legacy-service` — Kobie HTTP client
- `products-sfcc-legacy-service` — SFCC OCAPI client
- `tax-vertex-legacy-service` — Vertex-style HTTP client

Use **constructor injection**, **SLF4J** instead of `System.out.println` in new code, and **validated configuration properties** for externalized settings.
