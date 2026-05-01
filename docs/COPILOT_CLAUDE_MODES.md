# IDE custom agents — modernization workflow (no copy-paste)

Custom agents live in **`.github/agents/*.agent.md`**. Each file embeds the **system prompt** (Markdown body after YAML frontmatter). In **VS Code** with **GitHub Copilot** (or any editor that loads the same agent definitions), open the agent chat, pick an agent from the **dropdown** — you do **not** paste prompts manually.

**Handoffs:** After a response, use the **handoff buttons** to move to the next agent with context and a **pre-filled** prompt (you can edit before sending). This encodes the modernization **workflow**.

> **GitHub.com / CLI:** Custom agents load from the same folder; `handoffs` may be **VS Code–only** — if buttons are missing, select the next agent manually using the table below.

---

## Agent files and selection model

**Select only:** `p0-orchestrator.agent.md`

- `p0-orchestrator` is the user-facing workflow controller.
- All other agents are configured as `user-invocable: false` and are intended to run as sub-agents invoked by `p0`.
- If your IDE still shows hidden agents due to cache/version behavior, still use `p0` as the single entrypoint.
- Developer must provide **exactly one target service per run**.

| File | Role |
|------|------|
| `p0-orchestrator.agent.md` | Program sequencing, gates, risks |
| `p1-platform-eks.agent.md` | Docker, EKS, Helm, IRSA, secrets pattern |
| `p2-java-platform.agent.md` | Shared JDK / Spring / BOM strategy |
| `p3-architecture-api.agent.md` | API/versioning decisions (read-heavy) |
| `p4-security.agent.md` | Secret/logging/container review |
| `s1-tests.agent.md` | Contract tests from `docs/DEEPWIKI.md` |
| `s2-build-container.agent.md` | Dockerfile, CI |
| `s3-spring-upgrade.agent.md` | Spring Boot + jakarta per module |
| `s4-config-12factor.agent.md` | Env config, profiles, no secrets in Git |
| `s5-persistence.agent.md` | **customer-lookup only** — Flyway/RDS |
| `s6-http-clients.agent.md` | Outbound HTTP hardening |
| `s7-observability.agent.md` | Actuator, Micrometer, OTel, JSON logs |
| `s8-kubernetes.agent.md` | Deployment, probes, Ingress, IRSA |
| `s9-docs.agent.md` | Update `docs/DEEPWIKI.md` |

---

## Default automated workflow (independent service run)

1. Select **P0** and provide objective + **one** module scope.
2. P0 invokes sub-agents in order: **S1 → S3 → S4 → (S5 if customer) → S6 → S7 → S2 → S8 → S9**.
   - In **S3**, prefer OpenRewrite recipes for mechanical migration steps, then review and test.
3. P0 stops after the selected module is completed.
4. Developer starts a new run for the next module.

**Parallel tracks:** P0 can invoke **P1** / **P2** / **P3** / **P4** as specialized sub-agents for platform-wide, architecture, or security tasks.

---

## Always-on instructions

Repo-wide IDE / assistant context: **`.github/copilot-instructions.md`**.

Deep reference: **`docs/DEEPWIKI.md`**, **`docs/CLAUDE_MODERNIZATION_PLAYBOOK.md`**, **`docs/TWELVE_FACTOR.md`**, **`docs/MULTI_BRAND_ARCHITECTURE.md`**.

---

## Optional: `argument-hint`

Each agent sets **`argument-hint`** in frontmatter so the chat box reminds you what to type (e.g. module name).

---

## Official references

- [Custom agents in VS Code](https://code.visualstudio.com/docs/copilot/customization/custom-agents) (handoffs, `.github/agents`)
- [Custom agents configuration (GitHub Docs)](https://docs.github.com/en/copilot/reference/custom-agents-configuration)
