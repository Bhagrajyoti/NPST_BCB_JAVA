package com.bank.termdeposit.domain.entity;

import com.bank.termdeposit.domain.statemachine.TermDepositStatus;
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

/** UUID primary key, money as bigint minor units — same conventions as the funds-transfer schema. */
@Entity
@Table(
        name = "term_deposit",
        indexes = {
                @Index(name = "idx_term_deposit_cif", columnList = "cif"),
                @Index(name = "idx_term_deposit_status", columnList = "status"),
                @Index(name = "idx_term_deposit_maturity_date", columnList = "maturity_date, status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermDeposit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 20)
    private String cif;

    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(name = "source_account_number", nullable = false, length = 30)
    private String sourceAccountNumber; // CBS account debited to fund the deposit

    @Column(name = "principal_minor_units", nullable = false)
    private BigInteger principalMinorUnits;

    @Column(name = "interest_rate_bps", nullable = false)
    private Integer interestRateBps; // basis points, e.g. 650 = 6.50%

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "maturity_amount_minor_units", nullable = false)
    private BigInteger maturityAmountMinorUnits;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private boolean autoRenew = false;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TermDepositStatus status;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode; // tenant discriminator

    @Version
    private Long version; // MaturityProcessingScheduler and a live premature-closure request can race on this row

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
