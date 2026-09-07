package com.bank.ft.domain.entity;

import com.bank.ft.domain.statemachine.ScheduleFrequency;
import com.bank.ft.domain.statemachine.ScheduleStatus;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "scheduled_transfer",
        indexes = {
                @Index(name = "idx_scheduled_transfer_cif", columnList = "cif"),
                @Index(name = "idx_scheduled_transfer_next_execution", columnList = "next_execution_date, status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(length = 255)
    private String remarks;

    @Column(name = "last_execution_status", length = 20)
    private String lastExecutionStatus; // deliberately a plain string, not TransactionStatus — see schema doc's open item #5

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
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
}
