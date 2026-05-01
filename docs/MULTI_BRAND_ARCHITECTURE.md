# Multi-brand architecture for modernized services

This document defines how to make the services brand-configurable while keeping one maintainable codebase.

## Goal

- Support multiple brands with different providers/endpoints/DBs.
- Keep business logic shared and stable.
- Isolate brand variability in adapters and configuration.

## Simple mental model

- Canonicalization = one internal format.
- Adapters = translators for each provider.
- Brand config = runtime routing rules.

Example: loyalty for brand A uses Kobie, brand B uses SessionM.
Both are mapped to one internal `LoyaltyProfile` model before business logic uses the data.

## Canonicalization boundaries

Canonicalize these parts:

- Domain request/response models used by service core.
- Domain errors and status semantics.
- Core business decisions (eligibility, fallback policy, response shaping).

Do not canonicalize these parts:

- Provider auth details and headers.
- Provider request/response payload structure.
- Provider-specific retries/throttling nuances.

## Reference flow

1. Inbound request resolves `brandId` from trusted source (header, host, JWT claim).
2. `BrandConfigRegistry` loads brand policy/config (provider type, endpoint refs, DB refs, timeouts, fallback policy).
3. `ProviderRouter` selects adapter for the brand/provider.
4. Adapter maps canonical request -> provider payload.
5. Provider response maps back to canonical domain model.
6. Service returns API response preserving existing contract unless versioned.

## Loyalty example: Kobie vs SessionM

Canonical model:

- `LoyaltyLookupRequest { brandId, lookupType, lookupValue }`
- `LoyaltyProfile { memberId, pointsBalance, tier, certificates, status, asOf }`
- `LoyaltyError { code, message, retryable, provider }`

Adapters:

- `KobieLoyaltyAdapter implements LoyaltyProviderAdapter`
- `SessionMLoyaltyAdapter implements LoyaltyProviderAdapter`

Routing:

- Brand A config -> provider `KOBIE` -> `KobieLoyaltyAdapter`
- Brand B config -> provider `SESSION_M` -> `SessionMLoyaltyAdapter`

Only adapters know provider payload details; service core stays unchanged.

## Configuration model (12-factor)

Externalize all brand settings to environment/secret references.

Suggested structure (conceptual):

- `brands.<brandId>.enabled`
- `brands.<brandId>.loyalty.provider` (`KOBIE`, `SESSION_M`, ...)
- `brands.<brandId>.loyalty.baseUrl`
- `brands.<brandId>.loyalty.timeoutMs.connect/read`
- `brands.<brandId>.loyalty.secretRefs.*`
- `brands.<brandId>.customer.dbRef`
- `brands.<brandId>.fallbackPolicy` (`FAIL_OPEN_STUB`, `FAIL_CLOSED`, ...)

Do not commit provider secrets in repo. Bind secrets at deploy time (e.g. **AWS Secrets Manager** + **External Secrets Operator**, or your cloud’s equivalent).

## Data and DB strategy

- Start with one service codebase and brand-aware routing.
- For DB-backed modules, use brand-aware datasource routing where needed.
- Prefer database-per-brand for strong isolation; schema-per-brand only if operationally justified.
- Enforce strict tenant/brand guards in repository/service layer.

## Observability requirements (brand-aware)

Every service must include brand dimensions:

- Logs: `brandId`, `provider`, `requestId`, `traceId`.
- Metrics: tags `brandId`, `provider`, `outcome`.
- Traces: span attributes for brand/provider and upstream endpoint alias.
- Alerts: per-brand error-rate and latency SLOs for high-volume brands.

## Rollout strategy

1. Introduce canonical models and adapter interfaces.
2. Implement first adapter for current provider (no behavior change).
3. Add second provider adapter behind config.
4. Enable brand routing in one non-prod environment.
5. Add brand contract tests and per-brand synthetic checks.
6. Roll out brand-by-brand with feature flags.

## Decision guardrails

Use canonicalization + adapters when:

- Same business capability, different provider contracts by brand.
- You need consistency, testability, and easier onboarding of new brands.

Consider separate service/cell when:

- Brand requires strict isolation/compliance.
- Throughput/noise profile justifies dedicated deployment.

## Links

- `docs/DEEPWIKI.md`
- `docs/TWELVE_FACTOR.md`
- `docs/CLAUDE_MODERNIZATION_PLAYBOOK.md`
