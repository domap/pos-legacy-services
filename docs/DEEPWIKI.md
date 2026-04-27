# POS Legacy — Deepwiki (current state)

This document is the **source of truth for AI-assisted modernization**: paste or attach it when asking Claude (or other models) to generate modern replacements. It describes **behavior, APIs, dependencies, and known debt** as implemented today.

---

## Monorepo layout

| Module | Artifact (WAR) | Default port | Primary integration |
|--------|----------------|--------------|------------------------|
| `customer-lookup-legacy-service` | `customer-lookup-legacy.war` | 8081 | H2 file DB + JPA |
| `loyalty-kobie-legacy-service` | `loyalty-kobie-legacy.war` | 8082 | Kobie HTTP (stub/offline) |
| `products-sfcc-legacy-service` | `products-sfcc-legacy.war` | 8084 | Salesforce SFCC OCAPI |
| `tax-vertex-legacy-service` | `tax-vertex-legacy.war` | 8083 | Vertex-style HTTP/XML (stub) |

**Stack (all services):** Java 8, Spring Boot **2.7.18**, `spring-boot-starter-web`, packaging **WAR**, `spring-boot-starter-tomcat` **provided** (external Tomcat deployment). Main classes extend `SpringBootServletInitializer` where present.

**Observability today:** No Micrometer, Actuator, OpenTelemetry, or structured logging contract. Logging is mostly `System.out.println` in services/clients and some Spring `logging.level.*` in properties.

---

## Shared modernization facts (preserve externally visible behavior)

1. **JSON responses** are often `Map<String, Object>` (not DTOs). Modern code may introduce DTOs but must keep **URL paths, query param names, and status semantics** compatible unless explicitly versioning APIs (e.g. `/api/v2/...`).
2. **Integrations** degrade to **offline stubs** when remote calls fail (SFCC, Kobie, Vertex). Modern services should keep **deterministic fallback** behind config flags, not surprise 500s in dev.
3. **Configuration** is file-centric (`application.properties`) with placeholders; production should move to **environment variables** and **secrets stores** (12-factor III).

---

## Service: customer-lookup-legacy-service

### Purpose

CRUD-style **customer lookup** by email or telephone, backed by **JPA** and **H2 file database**.

### Dependencies (Maven)

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `com.h2database:h2` (runtime)
- `spring-boot-starter-tomcat` (provided)

### Persistence

- **Entity:** `com.legacy.customer.model.CustomerEntity` — table `customers`, fields: `id`, `email`, `telephone`, `firstName`, `lastName`. Uses **`javax.persistence`** (`javax.persistence.*`).
- **Repository:** `CustomerRepository` extends `JpaRepository<CustomerEntity, Long>` with:
  - `Optional<CustomerEntity> findByEmailIgnoreCase(String email)`
  - `List<CustomerEntity> findByTelephone(String telephone)`
- **Seed data:** `src/main/resources/data.sql` inserts two sample rows.

### Configuration (`application.properties`)

- `server.port=8081`
- `spring.application.name=customer-lookup-legacy`
- H2 **file** URL: `jdbc:h2:file:./data/customer-legacy-db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`
- JPA: `ddl-auto=update`, `show-sql=true`, `defer-datasource-initialization=true`, `spring.sql.init.mode=always`
- H2 console enabled at `/h2-console`
- `logging.level.org.springframework.web=DEBUG` (legacy noise)

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

- `findByTelephone`: caps list at `LegacyAppConstants.MAX_RESULTS` (**500**).
- `addCustomer`: requires non-empty `email`.
- `updateCustomer`: partial update by non-null fields only.
- Uses **`System.out.println`** for tracing (replace with structured logs + trace IDs in modern stack).

### Constants (`LegacyAppConstants`)

- `DEFAULT_DB_PATH` — documented JDBC fragment (not always used if properties override).
- `MAX_RESULTS = 500`

### Modernization notes for code generators

- Replace H2 file DB in production with **RDS** (or equivalent); keep H2 for **tests only**.
- Migrate `javax.persistence` → **`jakarta.persistence`** with Spring Boot 3+.
- Introduce **Flyway/Liquibase** instead of `ddl-auto=update` for prod.
- Add **Actuator** health (readiness = DB), **OpenTelemetry**, and **Micrometer** metrics (e.g. repository latency).

---

## Service: loyalty-kobie-legacy-service

### Purpose

Facade for **Kobie loyalty**: lookup by email or phone, returns points and certificates (plus raw Kobie payload).

### Dependencies (Maven)

- `spring-boot-starter-web`
- `spring-boot-starter-tomcat` (provided)

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
- `spring-boot-starter-tomcat` (provided)

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
- `spring-boot-starter-tomcat` (provided)

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
| Deploy | WAR + external Tomcat | JAR in container; K8s probes |
| DB | H2 file on disk for customer service | Managed DB prod; H2 test-only |
| Security | Bearer/trusted IDs in properties | Secrets Manager / ESO; IRSA for AWS |

---

## Quick reference: ports and context paths

Embedded run (`mvn spring-boot:run`): ports **8081, 8082, 8083, 8084** as above. WAR deployment to Tomcat may add **context path**; see each module’s `DEPLOY.txt`.

---

## Document maintenance

When legacy code changes, update this file **in the same PR** so Claude and human implementers stay aligned. For the **agent orchestration** and **observability rollout**, see `docs/CLAUDE_MODERNIZATION_PLAYBOOK.md`. For the **full twelve-factor mapping**, see `docs/TWELVE_FACTOR.md`.
