package com.bank.loan.domain.entity;

import com.bank.loan.domain.statemachine.LoanStatus;
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

/** UUID primary key, money as bigint minor units — same conventions as the funds-transfer schema. */
@Entity
@Table(
        name = "loan",
        indexes = {
                @Index(name = "idx_loan_cif", columnList = "cif"),
                @Index(name = "idx_loan_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 20)
    private String cif;

    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(name = "loan_type", nullable = false, length = 20)
    private String loanType; // PERSONAL, HOME, AUTO, EDUCATION

    @Column(name = "disbursement_account_number", nullable = false, length = 30)
    private String disbursementAccountNumber;

    @Column(name = "principal_minor_units", nullable = false)
    private BigInteger principalMinorUnits;

    @Column(name = "interest_rate_bps", nullable = false)
    private Integer interestRateBps; // basis points

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emi_minor_units")
    private BigInteger emiMinorUnits; // nullable until APPROVED, when the EMI schedule is finalized

    @Column(name = "outstanding_principal_minor_units")
    private BigInteger outstandingPrincipalMinorUnits; // nullable until DISBURSED

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode; // tenant discriminator

    @Version
    private Long version; // a Checker approving/rejecting and a repayment/default job can race on this row

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
