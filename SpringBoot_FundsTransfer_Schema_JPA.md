# Spring Boot — `funds-transfer-service` — JPA Entities

Standards applied throughout — consistent with the NestJS identity schema
pass, adapted for what's actually different here:

- `UUID` primary keys, not auto-increment — same IDOR reasoning as before.
- `created_at`/`updated_at` on every table, via `@CreatedDate`/`@LastModifiedDate` (`@EntityListeners(AuditingEntityListener.class)`).
- **Money is stored as `bigint`, in minor units (paise), never `decimal`/`float`.** This resolves the open money-precision question from the NestJS `identity` schema doc — ₹1,234.56 is stored as `123456`. Recommend updating `corporate_hierarchy.approvalLimit` on the Nest side to match this convention, so the two stacks never disagree on a rounding edge case.
- `@Version` (optimistic locking) on every entity a background job (`ReconciliationJob`) and a live user-facing write can race on.
- **CIF is a hard requirement here, not nullable** — unlike the identity schema, Funds Transfer only ever operates on an existing CBS account relationship. There's no "pending CIF" state that makes sense for a money transfer. `keycloakUserId` is still stored alongside, for cross-platform audit traceability (so an audit log entry can be correlated back to the same digital identity across both stacks), but `cif` is the business key here, and it's required.

---

## 1. `beneficiary`

```java
// domain/entity/Beneficiary.java
package com.bank.ft.domain.entity;

import jakarta.persistence.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public enum BeneficiaryStatus {
    PENDING_COOLING_PERIOD,
    ACTIVE,
    BLOCKED,
    DELETED
}

public enum TransferMode {
    IMPS, NEFT, RTGS, INTRA_BANK
}

@Entity
@Table(
    name = "beneficiary",
    indexes = {
        @Index(name = "idx_beneficiary_owner_cif", columnList = "owner_cif"),
        @Index(name = "idx_beneficiary_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Beneficiary {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_cif", nullable = false, length = 20)
    private String ownerCif; // required — beneficiaries only exist for an actual account holder

    @Column(name = "owner_keycloak_user_id", nullable = false)
    private UUID ownerKeycloakUserId; // cross-platform audit correlation

    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    @Column(name = "beneficiary_account_number", nullable = false, length = 30)
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_ifsc_code", nullable = false, length = 11)
    private String beneficiaryIfscCode;

    @Column(name = "beneficiary_bank_name", length = 100)
    private String beneficiaryBankName; // nullable — resolved via IFSC lookup, may not always succeed

    @Column(length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_mode", nullable = false, length = 20)
    private TransferMode transferMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BeneficiaryStatus status;

    @Column(name = "cooling_period_ends_at")
    private Instant coolingPeriodEndsAt; // nullable once status moves past PENDING_COOLING_PERIOD

    @Column(name = "daily_limit_minor_units")
    private BigInteger dailyLimitMinorUnits; // nullable — falls back to the bank/tenant default limit if unset

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode; // tenant discriminator

    @Version
    private Long version; // a Checker blocking a beneficiary and the owner using it in-flight is a real race

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // getters/setters omitted
}
```

**Ties directly to the frontend's Beneficiaries → Cooling Period page** —
`status = PENDING_COOLING_PERIOD` + `coolingPeriodEndsAt` is exactly what
that screen reads and displays. Confirm the cooling-period *duration* (24h?
48h? configurable per bank?) with whoever owns that requirement — it
should live in `BankConfigProperties` (tenant module), not hardcoded here.

---

## 2. `transaction` — the funds-transfer record itself

```java
// domain/entity/Transaction.java
package com.bank.ft.domain.entity;

import jakarta.persistence.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public enum TransactionStatus {
    INITIATED, PENDING, PROCESSING, SUCCESS, FAILED, REVERSED
}

@Entity
@Table(
    name = "transaction",
    indexes = {
        @Index(name = "idx_transaction_initiator_cif", columnList = "initiator_cif"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_reference", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_transaction_idempotency_key", columnList = "idempotency_key", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 40)
    private String transactionReference; // our own reference, generated at INITIATED

    @Column(name = "cbs_reference_number", length = 40)
    private String cbsReferenceNumber; // nullable until CBS/Switch confirms it

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey; // defense in depth alongside the generic IdempotencyRecord table (see §4)

    @Column(name = "initiator_cif", nullable = false, length = 20)
    private String initiatorCif;

    @Column(name = "initiator_keycloak_user_id", nullable = false)
    private UUID initiatorKeycloakUserId;

    @Column(name = "beneficiary_id")
    private UUID beneficiaryId; // nullable — a one-time payee not saved as a Beneficiary is still a valid transfer

    // Destination details are DENORMALIZED here, not just a join to Beneficiary —
    // a Transaction record must stay accurate and immutable even if the
    // Beneficiary is later edited or deleted.
    @Column(name = "destination_account_number", nullable = false, length = 30)
    private String destinationAccountNumber;

    @Column(name = "destination_ifsc_code", nullable = false, length = 11)
    private String destinationIfscCode;

    @Column(name = "amount_minor_units", nullable = false)
    private BigInteger amountMinorUnits; // paise, never a float/decimal

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_mode", nullable = false, length = 20)
    private TransferMode transferMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status; // transitions guarded by TransactionStateMachine — see domain/statemachine/

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(length = 255)
    private String remarks;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Version
    private Long version; // critical — ReconciliationJob and a live status check both write to this row

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // getters/setters omitted
}
```

**`TransactionStateMachine` enforces legal transitions** — e.g.
`INITIATED → PENDING → PROCESSING → SUCCESS/FAILED`, and only `SUCCESS →
REVERSED` is ever legal in reverse. Any other transition throws
`IllegalTransitionException`, caught by `GlobalExceptionHandler`. This is
what actually prevents the "reconciliation job and live status check
disagree" race the earlier review flagged — the `@Version` column makes the
race detectable, the state machine makes an illegal outcome of that race
impossible to persist.

---

## 3. `scheduled_transfer`

```java
// domain/entity/ScheduledTransfer.java
package com.bank.ft.domain.entity;

import jakarta.persistence.*;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public enum ScheduleFrequency {
    ONE_TIME, DAILY, WEEKLY, MONTHLY
}

public enum ScheduleStatus {
    ACTIVE, PAUSED, COMPLETED, CANCELLED, FAILED
}

@Entity
@Table(
    name = "scheduled_transfer",
    indexes = {
        @Index(name = "idx_scheduled_transfer_cif", columnList = "cif"),
        @Index(name = "idx_scheduled_transfer_next_execution", columnList = "next_execution_date, status")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ScheduledTransfer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 20)
    private String cif;

    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(name = "beneficiary_id", nullable = false)
    private UUID beneficiaryId;

    @Column(name = "amount_minor_units", nullable = false)
    private BigInteger amountMinorUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_mode", nullable = false, length = 20)
    private TransferMode transferMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleFrequency frequency;

    @Column(name = "next_execution_date", nullable = false)
    private LocalDate nextExecutionDate;

    @Column(name = "end_date")
    private LocalDate endDate; // nullable — open-ended recurring transfers are valid

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status;

    @Column(name = "last_execution_status", length = 20)
    private String lastExecutionStatus; // nullable, mirrors TransactionStatus of the most recent run

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0; // backs the PRD's "failed scheduled transfers notify and retry" requirement (US-11)

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Version
    private Long version; // ScheduledTransferSchedulerJob writes here on every execution attempt

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    // getters/setters omitted
}
```

---

## 4. `idempotency_record`

```java
// idempotency/IdempotencyRecord.java
package com.bank.ft.idempotency;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

public enum IdempotencyStatus {
    IN_PROGRESS, COMPLETED, FAILED
}

@Entity
@Table(
    name = "idempotency_record",
    indexes = @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
)
public class IdempotencyRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey; // same header/value the frontend and NestJS side send — contract must match exactly

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash; // sha256 of the request body — detects same-key-different-payload misuse

    @Lob
    @Column(name = "response_body")
    private String responseBody; // JSON, replayed verbatim on a retried request

    @Column(name = "http_status")
    private Integer httpStatus; // replay the original response code exactly, not assume 200

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt; // TTL, cleaned up by a scheduled job — 24–48h typical

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // getters/setters omitted
}
```

**This is what `IdempotencyAspect` reads/writes before the controller
method runs** — checked by request hash, so a retried request with the same
idempotency key but a genuinely different payload is treated as an error,
not silently replayed.

---

## 5. `outbox_event`

```java
// messaging/outbox/OutboxEvent.java
package com.bank.ft.messaging.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

public enum OutboxStatus {
    PENDING, SENT, FAILED
}

@Entity
@Table(
    name = "outbox_event",
    indexes = @Index(name = "idx_outbox_status", columnList = "status")
)
public class OutboxEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType; // e.g. "TRANSACTION", "BENEFICIARY"

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // e.g. "TRANSACTION_COMPLETED", "NOTIFICATION_REQUESTED"

    @Lob
    @Column(nullable = false)
    private String payload; // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    // getters/setters omitted
}
```

**Notification-on-completion goes through this table**, not a synchronous
`NotificationFeignClient` call inside the transfer flow — this is the fix
from the earlier review, made concrete as an actual schema now.

---

## Standing questions before this schema is final

1. **Cooling-period duration** — is it a fixed value (e.g. 24h) or
   configurable per bank/tenant? If configurable, it belongs in
   `BankConfigProperties`, not a hardcoded constant.
2. **Daily/per-transaction limits** — `Beneficiary.dailyLimitMinorUnits` is
   nullable with an implied "falls back to tenant default" — confirm that
   fallback actually lives somewhere (`BankConfigProperties`) and isn't
   assumed without a real default value defined.
3. **Idempotency key contract must match exactly across all three
   surfaces** — frontend (`Idempotency-Key` header, generated once per
   `ActionDialog` open), NestJS, and this service. Confirm the exact header
   name and TTL are the same everywhere — this was flagged as an open item
   in both other docs and still needs a single source of truth.
4. **Money precision decision made here (`bigint`, minor units) should be
   back-ported to the NestJS `identity` schema's `corporate_hierarchy.approvalLimit`**,
   which currently uses `numeric` as a string. Recommend fixing that for
   consistency before either side writes real migration code.
5. **`ScheduledTransfer.lastExecutionStatus` as a plain string, not the
   `TransactionStatus` enum** — deliberate, since a scheduled transfer's
   last attempt might fail before a real `Transaction` row even exists
   (e.g. CBS unreachable). Confirm this is an acceptable inconsistency, or
   decide it should instead always create a `Transaction` row (even a
   `FAILED` one) so `lastExecutionStatus` can be a proper FK-backed lookup
   instead of a denormalized string.
