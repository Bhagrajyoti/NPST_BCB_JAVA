CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE beneficiary (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_cif                   VARCHAR(20) NOT NULL,
    owner_keycloak_user_id      UUID NOT NULL,
    beneficiary_name            VARCHAR(100) NOT NULL,
    beneficiary_account_number  VARCHAR(30) NOT NULL,
    beneficiary_ifsc_code       VARCHAR(11) NOT NULL,
    beneficiary_bank_name       VARCHAR(100),
    nickname                    VARCHAR(50),
    transfer_mode               VARCHAR(20) NOT NULL,
    status                      VARCHAR(30) NOT NULL,
    cooling_period_ends_at      TIMESTAMPTZ,
    daily_limit_minor_units     BIGINT,
    bank_code                   VARCHAR(20) NOT NULL,
    version                     BIGINT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_beneficiary_owner_cif ON beneficiary (owner_cif);
CREATE INDEX idx_beneficiary_status ON beneficiary (status);

CREATE TABLE transaction (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_reference       VARCHAR(40) NOT NULL UNIQUE,
    cbs_reference_number        VARCHAR(40),
    idempotency_key             VARCHAR(128) NOT NULL UNIQUE,
    initiator_cif                VARCHAR(20) NOT NULL,
    initiator_keycloak_user_id  UUID NOT NULL,
    beneficiary_id               UUID,
    destination_account_number  VARCHAR(30) NOT NULL,
    destination_ifsc_code       VARCHAR(11) NOT NULL,
    amount_minor_units           BIGINT NOT NULL,
    currency                     VARCHAR(3) NOT NULL DEFAULT 'INR',
    transfer_mode                VARCHAR(20) NOT NULL,
    status                       VARCHAR(20) NOT NULL,
    failure_reason                VARCHAR(255),
    remarks                       VARCHAR(255),
    bank_code                     VARCHAR(20) NOT NULL,
    version                       BIGINT NOT NULL DEFAULT 0,
    initiated_at                  TIMESTAMPTZ NOT NULL,
    completed_at                  TIMESTAMPTZ,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transaction_initiator_cif ON transaction (initiator_cif);
CREATE INDEX idx_transaction_status ON transaction (status);

CREATE TABLE scheduled_transfer (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cif                   VARCHAR(20) NOT NULL,
    keycloak_user_id      UUID NOT NULL,
    beneficiary_id        UUID NOT NULL,
    amount_minor_units    BIGINT NOT NULL,
    transfer_mode         VARCHAR(20) NOT NULL,
    frequency             VARCHAR(20) NOT NULL,
    next_execution_date   DATE NOT NULL,
    end_date              DATE,
    status                VARCHAR(20) NOT NULL,
    last_execution_status VARCHAR(20),
    last_executed_at      TIMESTAMPTZ,
    retry_count           INTEGER NOT NULL DEFAULT 0,
    max_retries           INTEGER NOT NULL DEFAULT 3,
    bank_code             VARCHAR(20) NOT NULL,
    version               BIGINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scheduled_transfer_cif ON scheduled_transfer (cif);
CREATE INDEX idx_scheduled_transfer_next_execution ON scheduled_transfer (next_execution_date, status);

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
