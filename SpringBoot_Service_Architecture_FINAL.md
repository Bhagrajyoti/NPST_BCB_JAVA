# Spring Boot Services Architecture — `{domain}-service` (Final)

**Audience:** Java/Spring Boot team.
**Applies to:** `account-service`, `funds-transfer-service`,
`term-deposit-service`, `loan-service`, `other-services`. Same skeleton
every time — only the domain-specific folders inside `api/v1`, `domain/`,
and `client/` change per service.

**Status:** Single source of truth for these services. Read this before
writing feature code — it's short on purpose.

---

## 1. The core decision: separate microservices, one per banking domain

Unlike the NestJS side (Auth/Bill Payment/Admin — collapsed into one
modulith, see that team's doc), **each of these domains stays a genuinely
separate, independently deployable Spring Boot service.**

**Why the two stacks are treated differently:**
- Individually, Funds Transfer, Loans, Term Deposit, and Account each carry
  enough complexity, risk, and scale profile to justify their own
  deployment pipeline, health checks, and network boundary — Funds
  Transfer alone (IMPS, NEFT/RTGS) is rated Very Complex and is a mandatory
  security-review gate on its own.
- These domains are more likely to need independent scaling under load
  (e.g. Funds Transfer traffic patterns look nothing like Term Deposit's).
- This is the opposite reasoning from the NestJS side, not an inconsistency
  — the deciding factor both times was "does this domain individually
  justify the isolation," and for these five, yes.

**What this means in practice:** these are real, separate services from
day one — separate repos, separate pipelines, separate scaling policies.
No modulith shortcuts here.

---

## 2. Tooling standard

Spring Boot 3.x · Spring Security OAuth2 Resource Server (Keycloak as
issuer) · Spring Data JPA + Flyway · OpenFeign + Resilience4j (circuit
breaker/retry/timeout) · MapStruct (entity↔DTO mapping) · Lombok ·
springdoc-openapi · Spring Boot Actuator + Micrometer + OpenTelemetry ·
Testcontainers for integration tests.

---

## 3. Folder structure

```
{domain}-service/
├── pom.xml
├── Dockerfile
├── src/main/java/com/bank/{domain}/
│   ├── {Domain}ServiceApplication.java        # @EnableFeignClients @EnableDiscoveryClient
│   │
│   ├── api/
│   │   └── v1/                                # ★ versioned from the first controller
│   │       ├── controller/                    # one per domain entity
│   │       ├── dto/
│   │       │   ├── request/
│   │       │   └── response/
│   │       ├── mapper/                        # MapStruct interfaces, entity <-> DTO
│   │       └── openapi/                       # springdoc config
│   │
│   ├── domain/
│   │   ├── service/                           # business logic, one per domain entity
│   │   ├── statemachine/                      # wherever an entity has a real lifecycle
│   │   │   ├── {Entity}Status.java
│   │   │   └── {Entity}StateMachine.java       # throws on illegal transitions
│   │   ├── entity/                             # @Version on anything with concurrent writers
│   │   └── repository/
│   │
│   ├── idempotency/
│   │   ├── IdempotencyRecord.java              # key, request hash, response, status, expiry
│   │   ├── IdempotencyRepository.java
│   │   └── IdempotencyAspect.java              # @Around advice — runs before controller logic
│   │
│   ├── client/
│   │   ├── feign/                              # CbsFeignClient, SwitchFeignClient, NotificationFeignClient...
│   │   ├── fallback/                           # one fallback per Feign client, no exceptions
│   │   └── config/
│   │       ├── FeignClientConfig.java          # timeouts, retry policy
│   │       ├── Resilience4jConfig.java         # circuit breaker thresholds
│   │       └── ChecksumRequestInterceptor.java # signs outbound calls to NestJS services
│   │
│   ├── messaging/
│   │   ├── config/RabbitMqConfig.java
│   │   ├── producer/                           # AuditEventProducer, domain event producers
│   │   └── outbox/
│   │       ├── OutboxEvent.java
│   │       ├── OutboxEventRepository.java
│   │       └── OutboxRelayScheduler.java       # anything non-blocking goes through here
│   │
│   ├── tenant/
│   │   ├── TenantContext.java
│   │   ├── TenantResolverFilter.java           # resolves bank/tenant from header/subdomain/JWT claim
│   │   └── BankConfigProperties.java           # per-tenant CIF format, limits, etc.
│   │
│   ├── common/
│   │   ├── constant/
│   │   │   └── ErrorCodeCatalog.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java     # @RestControllerAdvice, maps to ErrorCodeCatalog
│   │   │   ├── CbsUnavailableException.java
│   │   │   ├── IllegalTransitionException.java
│   │   │   └── ChecksumMismatchException.java
│   │   ├── security/
│   │   │   ├── keycloak/
│   │   │   │   ├── KeycloakJwtAuthConverter.java   # maps Keycloak realm/client roles -> GrantedAuthority
│   │   │   │   ├── KeycloakRoleConstants.java       # role name constants, no magic strings
│   │   │   │   └── CurrentUserResolver.java         # pulls sub/CIF/roles off the validated JWT
│   │   │   ├── IdorGuard.java                       # checks resource owner vs. authenticated principal
│   │   │   └── checksum/
│   │   │       ├── ChecksumUtil.java                # HMAC-SHA256(payload + timestamp, secret)
│   │   │       ├── ChecksumProperties.java          # secret scoped per service-pair, sourced from vault
│   │   │       └── ChecksumValidationFilter.java    # validates inbound calls, scoped to /internal/** only
│   │   └── util/
│   │       └── MaskingUtil.java                     # enforced centrally in logging config, not opt-in
│   │
│   └── config/
│       ├── DataSourceConfig.java
│       ├── SecurityConfig.java                 # /api/v1/** -> JWT via Keycloak; /internal/** -> checksum only
│       ├── ObservabilityConfig.java            # Micrometer + OpenTelemetry
│       └── ActuatorConfig.java
│
├── src/main/resources/
│   ├── application.yml                         # keycloak.issuer-uri, realm, client-id
│   ├── application-{env}.yml                   # per-environment / per-tenant overrides
│   └── db/migration/                           # Flyway, versioned V1__, V2__ ...
│       └── V1__init_schema.sql
│
└── src/test/java/com/bank/{domain}/
    ├── controller/
    ├── service/
    ├── statemachine/                           # illegal-transition tests
    ├── security/
    │   ├── ChecksumTamperingTest.java
    │   ├── IdorAttemptTest.java
    │   └── IdempotencyReplayTest.java
    └── integration/                            # Testcontainers — real Postgres, real RabbitMQ
```

---

## 4. RBAC via Keycloak — how it actually works here

- Keycloak issues the JWT; `KeycloakJwtAuthConverter` maps the realm/client
  role claims to Spring `GrantedAuthority` objects — this is the one place
  role-mapping logic lives, not scattered across controllers.
- Controllers enforce access with `@PreAuthorize("hasRole('OPS')")` or
  `@RolesAllowed("OPS")`, using constants from `KeycloakRoleConstants` —
  never a raw string, never a manual claim check inside a service method.
- `CurrentUserResolver` pulls `sub`/CIF/roles off the already-validated JWT
  — controllers never trust a client-supplied CIF or user ID in the request
  body/params for anything security-relevant.
- `SecurityConfig` splits routes explicitly: `/api/v1/**` requires a valid
  Keycloak JWT; `/internal/**` requires a valid checksum instead (see §5).
  These are two different filter chains — don't let them overlap.

---

## 5. Non-negotiables

1. **API is versioned from the first commit** — `/api/v1/**`, no exceptions.
2. **RBAC is always `@PreAuthorize`/`@RolesAllowed` at the controller method
   level**, using `KeycloakRoleConstants` — never a raw string, never a
   manual claim check buried in business logic.
3. **Idempotency is a real stored record checked before the controller
   runs** (`IdempotencyAspect`), not a format validator checked inside the
   service method.
4. **Every Feign client has a fallback and sits under
   `Resilience4jConfig`** — no outbound call without timeout/retry/circuit
   breaker, no exceptions for "just this one internal call."
5. **Anything audit- or notification-related goes through
   `messaging/outbox/`**, never a synchronous Feign call sitting inside a
   transaction's critical path. If a customer notification is currently
   fired inline from a transfer/payment flow, that's a bug to fix, not a
   pattern to repeat in new code.
6. **Entity transitions with a lifecycle go through a state machine that
   throws on illegal transitions**, plus `@Version` for optimistic locking
   wherever a background job (e.g. `ReconciliationJob`) and a live
   user-facing write can race on the same row.
7. **`ChecksumValidationFilter` is scoped strictly to `/internal/**`** —
   never applied to customer-facing `/api/v1/**` routes, and never skipped
   on internal routes either.
8. **`tenant/` exists from day one**, even with one bank live today — this
   is what keeps the next client's onboarding a config change instead of a
   fork.
9. **Security-specific tests are mandatory for any domain rated Complex or
   above** (Funds Transfer, Loans) — checksum tampering, IDOR attempts,
   idempotency replay — not just controller/service happy-path coverage.
10. **Checksum secrets are scoped per service-pair, sourced from vault** —
    never one global shared secret across every inter-service call.

---

## 6. Open items — confirm before this is fully locked

| Item | Depends on | Status |
|---|---|---|
| Checksum contract (secret scoping, header names, which routes are `/internal/**`) | NestJS pod | **Must match exactly on both sides — confirm together, don't each guess** |
| Notification-on-completion moved to outbox instead of inline Feign call | Funds Transfer / Bill Payment flows currently doing this synchronously | **Audit existing flows, fix inline calls found** |
| Idempotency key header name/format | NestJS pod (frontend sends the same key through the gateway to whichever backend handles it) | **Must be one contract across both stacks, not two** |
| Tenant/bank config rollout — actual mechanism (header, subdomain, JWT claim) | Platform/gateway owner | **Decide once, apply identically in both stacks** |
| Transaction/entity state machine adoption per domain | Each domain's lead dev | **Confirm which entities in each service actually need this — not every entity has a meaningful lifecycle** |

---

## 7. Definition of done — before a service connects to real CBS/Switch or goes live

- [ ] `/api/v1/` prefix in place from the first controller
- [ ] `IdempotencyAspect` implemented and tested with an actual replay scenario, not just format validation
- [ ] Every Feign client has a fallback and is registered under `Resilience4jConfig`, proven to trip under simulated failure
- [ ] No synchronous Feign call sits inside a transaction's critical path for anything non-essential (notifications, audit) — verified by an explicit audit of existing flows, not just new code
- [ ] State machine guards in place for any entity with concurrent writers (live user action + background job)
- [ ] `ChecksumValidationFilter` confirmed scoped to `/internal/**` only, verified with a test hitting `/api/v1/**` without a checksum header (should succeed) and `/internal/**` without one (should fail)
- [ ] `tenant/` wired in, even if only one bank config exists today
- [ ] Security test suite (checksum tampering, IDOR, idempotency replay) present for any Complex-or-above domain
- [ ] Checksum secret sourced from vault, scoped per service-pair, not a single global value
- [ ] Checksum + idempotency-key contracts confirmed identical to what the NestJS team implemented
