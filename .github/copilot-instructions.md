# GitHub Copilot — pos-legacy modernization

This repo contains four **Spring Boot 2.7 / Java 8** WAR services being modernized (JDK target, JAR, 12-factor, EKS, observability).

## Always follow

1. **Source of truth for APIs and ports:** `docs/DEEPWIKI.md` — do not invent routes or response shapes.
2. **Twelve-factor:** `docs/TWELVE_FACTOR.md` — config via environment; no secrets in committed files.
3. **Orchestration & observability baseline:** `docs/CLAUDE_MODERNIZATION_PLAYBOOK.md` — Actuator readiness/liveness, Micrometer, OpenTelemetry, JSON logs.
4. **Multi-brand architecture:** `docs/MULTI_BRAND_ARCHITECTURE.md` — canonical models + provider adapters + brand-aware configuration.
5. **Scope:** Prefer one logical change set per task (one sub-agent scope). Preserve public HTTP contracts unless versioning is explicit (`/api/v2`).
6. **Minimum modernization gates per service:** Java/Spring upgrade, 12-factor config, containerization, ECR image publishing, AWS Secrets Manager integration, and EKS deployment readiness.

## Custom agents (workflow — no prompt copy-paste)

Specialist agents with **embedded system prompts** live in **`.github/agents/*.agent.md`**. In Copilot Chat, **select the agent from the dropdown**; use **handoff buttons** (VS Code) to move to the next step.

**How and when:** **`docs/COPILOT_CLAUDE_MODES.md`**

### Single-agent workflow

- Use **`p0-orchestrator`** as the only entrypoint.
- `p0-orchestrator` invokes all required sub-agents (`S1..S9`, `P1..P4`) as needed.
- Other agents are hidden from direct selection (`user-invocable: false`) and exist for orchestration.
- Each run is **independent per service**: developer must select one target module and request modernization for that module only.

## Services (modules)

- `customer-lookup-legacy-service` — JPA + H2 (modernize to RDS + Flyway in prod)
- `loyalty-kobie-legacy-service` — Kobie HTTP client
- `products-sfcc-legacy-service` — SFCC OCAPI client
- `tax-vertex-legacy-service` — Vertex-style HTTP client

Use **constructor injection**, **SLF4J** instead of `System.out.println` in new code, and **validated configuration properties** for externalized settings.
