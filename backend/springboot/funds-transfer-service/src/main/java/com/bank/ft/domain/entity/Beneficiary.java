package com.bank.ft.domain.entity;

import com.bank.ft.domain.statemachine.BeneficiaryStatus;
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

@Entity
@Table(
        name = "beneficiary",
        indexes = {
                @Index(name = "idx_beneficiary_owner_cif", columnList = "owner_cif"),
                @Index(name = "idx_beneficiary_status", columnList = "status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
