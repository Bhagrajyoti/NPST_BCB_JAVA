# NPST BCB — Spring Boot Services

Backend microservices for the banking domains that stay independently
deployable Spring Boot services (see
[`SpringBoot_Service_Architecture_FINAL.md`](SpringBoot_Service_Architecture_FINAL.md)
for the full rationale and the folder-structure contract every service
follows). Funds-transfer entity design comes from
[`SpringBoot_FundsTransfer_Schema_JPA.md`](SpringBoot_FundsTransfer_Schema_JPA.md).

## Services

| Service | Path | Package | Default port |
|---|---|---|---|
| Account | `backend/springboot/account-service` | `com.bank.account` | 8081 |
| Funds Transfer | `backend/springboot/funds-transfer-service` | `com.bank.ft` | 8082 |
| Term Deposit | `backend/springboot/term-deposit-service` | `com.bank.termdeposit` | 8083 |
| Loan | `backend/springboot/loan-service` | `com.bank.loan` | 8084 |

Each service is a standalone Maven project with its own `pom.xml`,
`Dockerfile`, Flyway migrations, and test suite — no shared parent module,
by design (see architecture doc §1). All four share the exact same folder
skeleton; only `api/v1/`, `domain/`, and `client/` differ per domain.

## Tooling (per the architecture doc §2)

Spring Boot 3.5.x · Spring Security OAuth2 Resource Server (Keycloak as
issuer) · Spring Data JPA + Flyway (PostgreSQL) · OpenFeign + Resilience4j
(circuit breaker/retry/timeout) · MapStruct · Lombok · springdoc-openapi ·
Spring Boot Actuator + Micrometer + OpenTelemetry · Testcontainers ·
RabbitMQ (messaging/outbox).

Verified dependency versions used in every `pom.xml` (Maven Central, current
as of this scaffold):

| Dependency | Version |
|---|---|
| `spring-boot-starter-parent` | 3.5.9 |
| `spring-cloud-dependencies` (BOM) | 2025.0.3 |
| `resilience4j-spring-boot3` / `resilience4j-feign` | 2.4.0 |
| `mapstruct` / `mapstruct-processor` | 1.6.3 |
| `springdoc-openapi-starter-webmvc-ui` | 2.8.13 |
| `testcontainers-bom` | 1.20.4 |
| `lombok` | managed by `spring-boot-starter-parent` |

## Prerequisites

- **JDK 21** (Spring Boot 3.5 baseline)
- **Maven 3.9+** (`mvn -v` to check)
- **Docker + Docker Compose** — for local Postgres/RabbitMQ/Keycloak, and
  required to run the integration test suites (Testcontainers spins up real
  Postgres + RabbitMQ containers per service)

## 1. Start local infrastructure

A single Compose file at `backend/springboot/docker-compose.yml` brings up
Postgres (pre-provisioned with one database + role per service — see
`backend/springboot/infra/postgres-init/`), RabbitMQ, and Keycloak in dev
mode:

```bash
cd backend/springboot
docker compose up -d
```

- Postgres: `localhost:5432` (superuser `postgres`/`postgres`; each service
  also gets its own DB/role, e.g. `account_service`/`changeme`)
- RabbitMQ management UI: http://localhost:15672 (`guest`/`guest`)
- Keycloak admin console: http://localhost:8080 (`admin`/`admin`)

## 2. Configure Keycloak

1. Log into the Keycloak admin console and create a realm named `bank`.
2. Create a confidential client per service (or one shared client for
   local dev) and add realm/client roles matching
   `KeycloakRoleConstants`: `CUSTOMER`, `OPS`, `CHECKER`, `ADMIN`.
3. Each service reads the issuer from `KEYCLOAK_ISSUER_URI`
   (default `http://localhost:8080/realms/bank`) — no per-service Keycloak
   config beyond that env var is required for local dev.

## 3. Build

From each service directory:

```bash
cd backend/springboot/account-service
mvn clean install
```

Or build all four:

```bash
for svc in account-service funds-transfer-service term-deposit-service loan-service; do
  (cd "backend/springboot/$svc" && mvn clean install)
done
```

## 4. Run a service

```bash
cd backend/springboot/account-service
mvn spring-boot:run
```

Or via Docker:

```bash
cd backend/springboot/account-service
docker build -t account-service .
docker run --network host account-service
```

Once running:
- Swagger UI: `http://localhost:<port>/swagger-ui.html`
- OpenAPI JSON: `http://localhost:<port>/v3/api-docs`
- Health: `http://localhost:<port>/actuator/health`

## 5. Run tests

```bash
cd backend/springboot/account-service
mvn test
```

Integration tests (`src/test/java/.../integration/`) use Testcontainers and
need Docker running — no other setup required, they provision their own
Postgres/RabbitMQ containers per test class. Security tests
(`src/test/java/.../security/`) cover checksum tampering, IDOR attempts, and
idempotency replay per the architecture doc's Definition of Done (§7).

## Adding the next domain service

The architecture doc's service list ends in "`other-services`" — a
placeholder for whatever domain comes after these four, not a literal
service. To add one:

1. Copy an existing service directory as a starting skeleton (`account-service`
   is the simplest reference; `funds-transfer-service` shows the pattern for
   a domain with multiple entities and an extra external Feign client).
2. Rename the package (`com.bank.<domain>`), the Maven `artifactId`/`name`,
   the Spring `application.name`, and the `ErrorCodeCatalog` prefix.
3. Replace the domain-specific files only: `domain/entity`,
   `domain/statemachine`, `domain/repository`, `domain/service`,
   `api/v1/controller`, `api/v1/dto`, `api/v1/mapper`, and any
   domain-specific `client/feign` clients + fallbacks.
4. Everything else — `idempotency/`, `messaging/outbox/`, `tenant/`,
   `common/`, top-level `config/` — carries over unchanged; that's the
   entire point of the shared skeleton (architecture doc §1, §3).
5. Add the new service to `backend/springboot/docker-compose.yml`'s
   Postgres init script and pick an unused port.

## Non-negotiables checklist

Before any of these services connects to a real CBS/Switch or goes live,
confirm every item in the architecture doc's §7 Definition of Done —
notably the checksum + idempotency-key contracts must match the NestJS
side exactly (doc §6 lists what's still open there).

#end
