CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE account (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number      VARCHAR(30) NOT NULL UNIQUE,
    cif                 VARCHAR(20) NOT NULL,
    keycloak_user_id    UUID NOT NULL,
    account_type        VARCHAR(20) NOT NULL,
    balance_minor_units BIGINT NOT NULL DEFAULT 0,
    currency            VARCHAR(3) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    bank_code           VARCHAR(20) NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_cif ON account (cif);
CREATE INDEX idx_account_status ON account (status);

CREATE TABLE idempotency_record (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    request_hash    VARCHAR(64) NOT NULL,
    response_body   TEXT,
    http_status     INTEGER,
    status          VARCHAR(20) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE outbox_event (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    retry_count    INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at        TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status ON outbox_event (status);
