# Legacy POS monorepo — Deepwiki (current state)

This document is the **source of truth for AI-assisted modernization**: attach or paste it when using an **AI coding assistant** (or other tools) so generated changes match current behavior. It describes **APIs, ports, dependencies, and known debt** as implemented today.

For **module summaries, class-level explanations, architecture narratives, and Mermaid diagrams** (dependency, call graph, data flow) and how to render them to **SVG** for a UI, see **`docs/DEEPWIKI_ARCHITECTURE.md`**.

---

## Monorepo layout

**Post-migration (program target):** every service ships as an **executable JAR** inside an **OCI container image**. **WAR files and external Tomcat are out of scope** for CI/CD and Kubernetes — only **JAR + `java -jar`** (or equivalent) on the release path. Until each module finishes packaging work, loyalty/products/tax **Maven builds may still emit a `.war`**; treat that as transitional, not the end state.

| Module | Runtime artifact (target) | Default port | Primary integration |
|--------|---------------------------|--------------|------------------------|
| `customer-lookup-legacy-service` | `customer-lookup-legacy.jar` | 8081 | Postgres (prod) / H2 (dev & test) + JPA + Flyway |
| `loyalty-kobie-legacy-service` | `loyalty-kobie-legacy.jar` | 8082 | Kobie HTTP (stub/offline) |
| `products-sfcc-legacy-service` | `products-sfcc-legacy.jar` | 8084 | Salesforce SFCC OCAPI |
| `tax-vertex-legacy-service` | `tax-vertex-legacy.jar` | 8083 | Vertex-style HTTP/XML (stub) |

**Stack:** `customer-lookup-legacy-service` is on **Java 25**, Spring Boot **3.5.x**, **`jakarta.persistence`**, executable **JAR** (embedded Tomcat). The other three modules remain **Java 8**, Spring Boot **2.7.18**, **`javax`**, **WAR** + `spring-boot-starter-tomcat` **provided**; main classes extend `SpringBootServletInitializer` where present. **After migration:** same **JAR-in-container** posture for **all four** — remove WAR packaging and **provided** Tomcat in favor of embedded Tomcat in the fat JAR (or equivalent) for every module.

**Observability:** `customer-lookup-legacy-service` has **Actuator** (liveness/readiness + DB), **Micrometer Prometheus**, **OTLP tracing** hooks, and **JSON logs** in the `prod` profile. The other three services still use ad-hoc logging without Actuator/OTel.

### Migration program verification (targets vs. this repo)

| Pillar | Program target | `customer-lookup-legacy-service` | `loyalty-kobie` / `products-sfcc` / `tax-vertex` |
|--------|----------------|----------------------------------|--------------------------------------------------|
| **JDK** | **25** (LTS line for modernized services) | **`java.version` 25** in `pom.xml`; CI uses Temurin **25** | **Java 8** until **S3** + platform JDK gate |
| **Spring Boot** | **3.5.x** on **Jakarta** for upgraded modules | Parent **3.5.11** | **2.7.18** / **javax** until **S3** |
| **12-factor** | Config from **env** / secret refs; no secrets in Git; profiles `dev`/`test`/`prod` | **`application.yml` + profiles**, `SPRING_DATASOURCE_*`, `CustomerModuleProperties` | File `application.properties`; migrate in **S4** |
| **Multi-brand** | Brand-aware config + safe routing per `docs/MULTI_BRAND_ARCHITECTURE.md` | **`app.customer.brands.*`** + startup validation; **no** inbound `brandId` routing or per-brand datasource wiring yet (**extend in P3/S4/S5**) | N/A until those modules adopt brand model |
| **Observability** | Actuator **liveness/readiness**, **Micrometer**, **OTel** export, **JSON** logs in prod | Implemented + **tests** hit `/actuator/health/*` and `/actuator/prometheus` | Not started — **S7** after **S3** |

**Build note:** Local `./mvnw verify` for **customer-lookup** requires a **JDK 25** toolchain on `PATH` / `JAVA_HOME` (same as GitHub Actions `setup-java`).

---

## Shared modernization facts (preserve externally visible behavior)

1. **JSON responses** are often `Map<String, Object>` (not DTOs). Modern code may introduce DTOs but must keep **URL paths, query param names, and status semantics** compatible unless explicitly versioning APIs (e.g. `/api/v2/...`).
2. **Integrations** degrade to **offline stubs** when remote calls fail (SFCC, Kobie, Vertex). Modern services should keep **deterministic fallback** behind config flags, not surprise 500s in dev.
3. **Configuration:** `customer-lookup-legacy-service` uses **`application.yml` + profiles** (`dev`, `prod`, `test`) with env overrides (`SPRING_DATASOURCE_*`, `SERVER_PORT`, `OTEL_*`, `app.customer.*`). The other services remain file-centric properties with placeholders; production should use **environment variables** and **secrets stores** (12-factor III).
4. **Packaging / deploy:** Target for **every** module is an **executable JAR** published as a **container image** — **no WAR** artifacts or external servlet containers on the **release** path.

---

## Service: customer-lookup-legacy-service

### Purpose

CRUD-style **customer lookup** by email or telephone, backed by **JPA**, **Flyway** schema (`db/migration`), and **H2** (dev file / test mem) or **PostgreSQL** (prod).

### Dependencies (Maven)

- `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`
- `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`
- `flyway-core`, `flyway-database-postgresql`
- `com.h2database:h2` (runtime — dev/test), `org.postgresql:postgresql` (runtime — prod)
- `net.logstash.logback:logstash-logback-encoder` (JSON logs in `prod`)
- Maven **`packaging: jar`** — executable fat JAR for **containers** (program-wide **no WAR** on the release path).

### Persistence

- **Entity:** `com.legacy.customer.model.CustomerEntity` — table `customers`, fields: `id`, `email`, `telephone`, `firstName`, `lastName`. Uses **`jakarta.persistence`** (`jakarta.persistence.*`).
- **Repository:** `CustomerRepository` extends `JpaRepository<CustomerEntity, Long>` with:
  - `Optional<CustomerEntity> findByEmailIgnoreCase(String email)`
  - `List<CustomerEntity> findByTelephone(String telephone)`
- **Migrations:** `src/main/resources/db/migration/V1__customers_table.sql`
- **Seed data:** `src/main/resources/data.sql` runs only for **embedded** databases (`spring.sql.init.mode=embedded`), i.e. dev H2 and test H2 — not for prod Postgres.

### Configuration (`application.yml` + profiles)

- Default profile **`dev`**: H2 file URL (override with `SPRING_DATASOURCE_URL`), H2 console `/h2-console`, `spring.jpa.show-sql=true`
- **`prod`**: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` required; Postgres driver; `ddl-auto=validate` (base `application.yml`)
- **`app.customer.max-results`** (default 500) and **`app.customer.brands.*`** — validated `CustomerModuleProperties` (multi-brand shape per `docs/MULTI_BRAND_ARCHITECTURE.md`); startup fails if no enabled brand
- **Actuator:** `/actuator/health/liveness`, `/actuator/health/readiness` (includes **db**), `/actuator/prometheus`; OTLP endpoint `management.otlp.tracing.endpoint` / `OTEL_EXPORTER_OTLP_ENDPOINT`
- **Port:** `SERVER_PORT` (default **8081**)

### HTTP API (`CustomerLookupController` — base `/api/customer`)

| Method | Path | Params / body | Response |
|--------|------|-----------------|------------|
| GET | `/api/customer/lookup/email` | `email` | 200 + map with `status=OK` and fields; **404** if `status=NOT_FOUND` |
| GET | `/api/customer/lookup/telephone` | `telephone` | 200 + map: `status`, `count`, `customers` (list of maps); **404** if not found |
| POST | `/api/customer/add` | JSON `CustomerEntity` | 200 on success; **400** if `status=ERROR` (e.g. missing email) |
| PUT | `/api/customer/update/{id}` | JSON partial `CustomerEntity` | 200; **404** if id missing |

### Response shape (legacy maps)

- **Success (single):** `status=OK`, `id`, `email`, `telephone`, `firstName`, `lastName`
- **Not found:** `status=NOT_FOUND`, `field`, `value`
- **Telephone multi:** `status=OK`, `count`, `customers` (each item same shape as single without nested status on inner maps — inner maps from `toLegacyMap` include `status=OK`)
- **Error:** `status=ERROR`, `message` (e.g. `email required`)

### Business rules (`CustomerLookupService`)

- `findByTelephone`: caps list at **`app.customer.max-results`** (default **500**).
- `addCustomer`: requires non-empty `email`.
- `updateCustomer`: partial update by non-null fields only.
- Uses **SLF4J** (`DEBUG`) instead of `System.out.println`.

### Constants (`LegacyAppConstants`)

- `DEFAULT_DB_PATH` — documented JDBC fragment only.
- `MAX_RESULTS` — deprecated; use **`app.customer.max-results`**.

### Deploy / CI

- **Dockerfile** and **`.dockerignore`** in module root; **Helm** chart `deploy/helm/customer-lookup/`.
- **CI (GitHub Actions):** `.github/workflows/customer-lookup-legacy-service.yml` — `./mvnw verify`, optional filesystem scan (Trivy), optional **image push** to your registry (example: **Amazon ECR** using OIDC — repository configures `AWS_ROLE_ARN` secret and `AWS_REGION` / `AWS_ECR_REPOSITORY` variables).

### Modernization notes for code generators

- H2 is for **dev embedded** and **tests** only; prod expects **RDS** credentials from secrets.
- Further hardening: repository-level metrics, distributed tracing sampling by env, Pod Security / NetworkPolicy in chart overlays.

---

## Service: loyalty-kobie-legacy-service

### Purpose

Facade for **Kobie loyalty**: lookup by email or phone, returns points and certificates (plus raw Kobie payload).

### Dependencies (Maven)

- `spring-boot-starter-web`
- `spring-boot-starter-tomcat` (provided) — **interim WAR layout**; **target** is **`jar`** packaging with default (embedded) Tomcat for **container-only** deploy (**no WAR** on release path).

### Configuration (`application.properties`)

- `server.port=8082`
- `spring.application.name=loyalty-kobie-legacy`
- `kobie.api.baseUrl=http://localhost:9999/kobie-mock`

### HTTP API (`LoyaltyController` — base `/api/loyalty`)

| Method | Path | Params | Response |
|--------|------|--------|------------|
| GET | `/api/loyalty/customer/email` | `email` | JSON map (always 200 from controller — no 404 mapping) |
| GET | `/api/loyalty/customer/phone` | `phone` | JSON map |

### Response shape (`LoyaltyQueryService.wrapResponse`)

Top-level keys: `email`, `phone`, `loyaltyPoints`, `certificates`, `kobieRaw` (full map from client).

### Integration (`KobieLoyaltyClient`)

- Property: `kobie.api.baseUrl` (default mock URL).
- Builds URL: `{base}/member/lookup?email=` or `...?phone=`
- Uses **`RestTemplate`** bean `kobieRestTemplate` from `KobieIntegrationConfig`: connect 3000 ms, read 5000 ms.
- On failure: **`OFFLINE_STUB`** map with `loyaltyPoints` 1250 and one certificate `CERT-GIFT-10` / `10_OFF`.
- **`System.out.println`** for URL and errors.

### Modernization notes

- Prefer **`RestClient`** or WebClient with **observable** timeouts; externalize **auth** when Kobie contract is known.
- Add **RED metrics** (rate, errors, duration) on outbound client span.
- Consider normalizing **phone query param** name (`phone` vs `telephone` in other services) when versioning APIs.

---

## Service: products-sfcc-legacy-service

### Purpose

**SFCC OCAPI**-style product search and product-by-id (implemented via search + match).

### Dependencies (Maven)

- `spring-boot-starter-web`
- `spring-boot-starter-tomcat` (provided) — **interim WAR layout**; **target** is **`jar`** + embedded Tomcat, **OCI image** only (**no WAR** on release path).

### Configuration (`application.properties`)

- `server.port=8084`
- `spring.application.name=products-sfcc-legacy`
- `sfcc.host`, `sfcc.ocapi.version` (default `v21_3`), `sfcc.clientId`, `sfcc.bearerToken` (placeholder)

### HTTP API (`ProductController` — base `/api/products`)

| Method | Path | Params | Response |
|--------|------|--------|------------|
| GET | `/api/products/search` | `q` (default `*`) | Map: `source=SFCC_OCAPI`, `query`, `hits` (list of maps) |
| GET | `/api/products/{id}` | path `id` | Map: `source=SFCC_OCAPI`, `product` (map) |

### Integration (`SfccOcapiClient`)

- Uses **anonymous `new RestTemplate()`** (no shared timeout factory — modernization debt).
- GET URL pattern: `{host}/s/SiteGenesis/dw/shop/{ocapiVersion}/product_search?q={query}&client_id={clientId}`
- **Bearer** auth header with `sfcc.bearerToken`.
- On failure: **`offlineCatalog`** returns two stub products (`SKU-1001`, `SKU-1002`).
- `getProductById`: delegates to `searchProducts(productId)` and picks matching `id` or first hit.

### Modernization notes

- Never log raw bearer tokens; use **masked** logging (already partially attempted in one log line).
- Align OCAPI path with **real site ID** / pipeline (SiteGenesis is illustrative).
- Add **resilience** (retry on 429/5xx with backoff), **circuit breaker**, and **trace propagation** into SFCC call.

---

## Service: tax-vertex-legacy-service

### Purpose

**Line tax calculation** placeholder integrating a **Vertex-like** HTTP/XML endpoint with local stub math.

### Dependencies (Maven)

- `spring-boot-starter-web`
- `spring-boot-starter-tomcat` (provided) — **interim WAR layout**; **target** is **`jar`** + embedded Tomcat, **OCI image** only (**no WAR** on release path).

### Configuration (`application.properties`)

- `server.port=8083`
- `spring.application.name=tax-vertex-legacy`
- `vertex.endpoint`, `vertex.trustedId` (placeholder secret)

### HTTP API (`TaxController` — base `/api/tax`)

| Method | Path | Params | Response |
|--------|------|--------|------------|
| GET | `/api/tax/calculate` | `amount` (String → `BigDecimal`), `postalCode` (default `78701`), `region` (default `TX`) | Map merged from Vertex client |

### Integration (`VertexSoapStyleClient`)

- **POST** `vertexEndpoint` with `Content-Type: text/xml`, body from **`buildFakeXmlEnvelope`** (string-concat pseudo-SOAP — **not** production-safe).
- On 2xx with body: merges parsed fragment map with `stubTax` output.
- On failure: **`stubTax`** only.
- **Stub rates:** default `0.0725`; if `region` contains `TX` (case-insensitive), `0.0625`.
- Response keys (stub path): `source=VERTEX_STUB`, `taxableAmount`, `taxAmount`, `effectiveRate`, `jurisdiction`
- **No timeouts** on default `RestTemplate` (critical modernization item).

### Modernization notes

- Replace pseudo-SOAP with **official Vertex SDK** or contract-tested client; **secrets** from vault only.
- Use **`BigDecimal`** end-to-end; validate `amount` input (format, scale).
- Strong **observability** on external call vs stub path (metric label `outcome=stub|live`).

---

## Cross-cutting anti-patterns (fix in modern services)

| Area | Current pattern | Target |
|------|-----------------|--------|
| Config | Properties files with defaults/secrets | Env + secret refs; validated `@ConfigurationProperties` |
| Logging | `System.out.println` | SLF4J JSON + trace/span IDs |
| HTTP client | Raw `RestTemplate`, uneven timeouts | `RestClient`/WebClient + OTel + metrics |
| Deploy | WAR + external Tomcat (legacy only, being removed) | **Executable JAR** in **OCI image** only; **Kubernetes** probes; **no WAR** on the release path |
| DB | **customer-lookup:** H2 file (typical dev), H2 in-memory (tests); legacy defaults in other modules | **customer-lookup:** managed DB in prod from env; H2 only where profiles use embedded DB; other modules → same pattern as they migrate |
| Security | Bearer/trusted IDs in properties | Secret store + External Secrets (or equivalent); workload identity for cloud APIs (e.g. IRSA on AWS) |

---

## Quick reference: ports and context paths

**Local run:** see each module’s **`DEPLOY.txt`**. Ports **8081, 8082, 8083, 8084** match the layout table above. **customer-lookup-legacy-service** ships **`./mvnw`** (Maven Wrapper); use `./mvnw spring-boot:run` from that module directory. Other modules may use `mvn spring-boot:run` until a wrapper is added. **Program target:** run every service from the **fat JAR** (same artifact as production containers); avoid relying on **external Tomcat** or **WAR** drops except during the short transition where a module has not yet switched `pom.xml` packaging to **`jar`**.

---

## Document maintenance

When legacy code changes, update this file **in the same PR** so **automation, AI assistants, and human implementers** stay aligned. For the **agent orchestration** and **observability rollout**, see `docs/CLAUDE_MODERNIZATION_PLAYBOOK.md`. For the **full twelve-factor mapping**, see `docs/TWELVE_FACTOR.md`. For **architecture Deepwiki** (diagrams + narratives), see `docs/DEEPWIKI_ARCHITECTURE.md`.
