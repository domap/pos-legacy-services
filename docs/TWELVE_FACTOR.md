# Twelve-factor app — mapping for this legacy POS monorepo

This document is the **explicit 12-factor checklist** for modernizing `customer-lookup`, `loyalty-kobie`, `products-sfcc`, and `tax-vertex`. **Stack and maturity differ by module** until each service finishes its vertical slice; use `docs/DEEPWIKI.md` (current behavior) and `docs/CLAUDE_MODERNIZATION_PLAYBOOK.md` (agents and observability).

---

## I. Codebase

**Target:** One codebase per app; many deploys via config only.

| Today | Modern |
|-------|--------|
| Four separate Maven modules (good) | Keep one repo; tag releases per service image |

---

## II. Dependencies

**Target:** Declare all dependencies explicitly (Maven); no reliance on preinstalled Tomcat in prod.

| Today | Modern |
|-------|--------|
| WAR + **provided** Tomcat (interim, three modules) | **Executable JAR** + embedded Tomcat in **OCI image** for **every** module — **no WAR** on CI/CD or cluster paths once migration completes |

---

## III. Config

**Target:** Store config in the **environment**; strict separation of config from code.

| Today | Modern |
|-------|--------|
| `application.properties` with defaults, placeholders, file DB paths | `SPRING_*` / custom env vars; **Secrets Manager** / External Secrets for tokens (`sfcc.bearerToken`, `vertex.trustedId`, DB passwords) |
| `LegacyAppConstants` for env-like values | Remove or replace with validated `@ConfigurationProperties` from env |

---

## IV. Backing services

**Target:** Treat DB, SFCC, Kobie, Vertex as **attached resources**; swap via URL/creds.

| Today | Modern |
|-------|--------|
| H2 **file** path in dev-style config (**customer-lookup** and similar) | **customer-lookup:** managed DB URL from env in prod; H2 only for embedded/dev/test profiles; other modules → same pattern as they migrate |
| Hardcoded fallbacks in code comments | Same **interface**, different endpoints per env |

---

## V. Build, release, run

**Target:** Strict pipeline: **build** → immutable **release** (image digest) → **run** in **Kubernetes** (e.g. **EKS**).

| Today | Modern |
|-------|--------|
| `mvn package` + copy **WAR** to Tomcat (legacy) | CI builds **JAR** image; Helm/GitOps applies manifest; **no WAR** or external servlet container on the release path |

---

## VI. Processes

**Target:** Stateless processes; share-nothing; scale out with replicas.

| Today | Modern |
|-------|--------|
| Apps are HTTP services (mostly stateless) | No session affinity unless required; externalize any future session state |

---

## VII. Port binding

**Target:** Service is self-contained; exports HTTP via **port binding**.

| Today | Modern |
|-------|--------|
| External Tomcat binds port | Container runs app on `$PORT` or fixed container port; K8s **Service** routes traffic |

---

## VIII. Concurrency

**Target:** Scale via **process model** (more pods), not vertical “bigger server” only.

| Today | Modern |
|-------|--------|
| Single-instance mindset | **HPA** on CPU/RPS/custom metrics; tune thread pools and DB pools per pod size |

---

## IX. Disposability

**Target:** Fast start, graceful **SIGTERM** shutdown.

| Today | Modern |
|-------|--------|
| No K8s-oriented health contract | **Liveness** vs **readiness** probes; Actuator; drain in-flight requests on shutdown |

---

## X. Dev/prod parity

**Target:** Same deploy artifact and similar topology across dev/stage/prod.

| Today | Modern |
|-------|--------|
| H2 file vs prod DB diverges heavily | **Same Docker image**; profile only for non-secret toggles; local **kind**/dev cluster with same chart |

---

## XI. Logs

**Target:** Treat logs as **event streams** on **stdout/stderr**; no log files as source of truth.

| Today | Modern |
|-------|--------|
| `System.out.println`, file-backed H2 | **JSON logs** to stdout; aggregation in your log stack (e.g. CloudWatch, OpenSearch, vendor); correlation with traces |

---

## XII. Admin processes

**Target:** One-off tasks as separate jobs, not `kubectl exec` as primary workflow.

| Today | Modern |
|-------|--------|
| H2 console, ad-hoc SQL | **Kubernetes Job** / migration Job (Flyway) / admin CLI image |

---

## Quick verification (per service)

- [ ] **III:** No secrets in Git; prod values only from env/secret store  
- [ ] **IV:** Customer DB is managed DB in prod; integrations URL-configurable  
- [ ] **V:** Image digest promoted by pipeline  
- [ ] **IX:** Readiness fails when critical backing service is unavailable (policy choice)  
- [ ] **XI:** No reliance on rotating files under `./data/` in prod for customer service  

---

## References

- [The Twelve-Factor App](https://12factor.net/) (official site)  
- `docs/DEEPWIKI.md` — current implementation and anti-patterns  
- `docs/CLAUDE_MODERNIZATION_PLAYBOOK.md` — **S4** implements much of III, IV, XI in code changes  
