package com.bank.ft.domain.entity;

import com.bank.ft.domain.statemachine.TransactionStatus;
import com.bank.ft.domain.statemachine.TransferMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** The funds-transfer record itself. Status transitions are guarded by {@code TransactionStateMachine}. */
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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 40)
    private String transactionReference; // our own reference, generated at INITIATED

    @Column(name = "cbs_reference_number", length = 40)
    private String cbsReferenceNumber; // nullable until CBS/Switch confirms it

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey; // defense in depth alongside the generic IdempotencyRecord table

    @Column(name = "initiator_cif", nullable = false, length = 20)
    private String initiatorCif;

    @Column(name = "initiator_keycloak_user_id", nullable = false)
    private UUID initiatorKeycloakUserId;

    @Column(name = "beneficiary_id")
    private UUID beneficiaryId; // nullable — a one-time payee not saved as a Beneficiary is still a valid transfer

    // Destination details are DENORMALIZED here, not just a join to Beneficiary — a Transaction
    // must stay accurate and immutable even if the Beneficiary is later edited or deleted.
    @Column(name = "destination_account_number", nullable = false, length = 30)
    private String destinationAccountNumber;

    @Column(name = "destination_ifsc_code", nullable = false, length = 11)
    private String destinationIfscCode;

    @Column(name = "amount_minor_units", nullable = false)
    private BigInteger amountMinorUnits; // paise, never a float/decimal

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_mode", nullable = false, length = 20)
    private TransferMode transferMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

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
}
