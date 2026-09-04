CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE term_deposit (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cif                         VARCHAR(20) NOT NULL,
    keycloak_user_id           UUID NOT NULL,
    source_account_number      VARCHAR(30) NOT NULL,
    principal_minor_units      BIGINT NOT NULL,
    interest_rate_bps          INTEGER NOT NULL,
    tenure_months              INTEGER NOT NULL,
    maturity_date              DATE NOT NULL,
    maturity_amount_minor_units BIGINT NOT NULL,
    auto_renew                 BOOLEAN NOT NULL DEFAULT false,
    currency                   VARCHAR(3) NOT NULL,
    status                     VARCHAR(30) NOT NULL,
    bank_code                  VARCHAR(20) NOT NULL,
    version                    BIGINT NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_term_deposit_cif ON term_deposit (cif);
CREATE INDEX idx_term_deposit_status ON term_deposit (status);
CREATE INDEX idx_term_deposit_maturity_date ON term_deposit (maturity_date, status);

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
