package com.bank.loan.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A real stored record, checked before the controller runs — see
 * {@link IdempotencyAspect}. Not a format validator inside a service method.
 */
@Entity
@Table(
        name = "idempotency_record",
        indexes = @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash; // sha256 of the request body — same key + different payload is an error, not a replay

    @Lob
    @Column(name = "response_body")
    private String responseBody; // JSON, replayed verbatim on a retried request

    @Column(name = "http_status")
    private Integer httpStatus; // replay the original response code exactly, not assume 200

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt; // TTL, cleaned up by a scheduled job — 24-48h typical

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
