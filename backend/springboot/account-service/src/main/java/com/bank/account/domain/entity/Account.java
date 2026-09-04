package com.bank.account.domain.entity;

import com.bank.account.domain.statemachine.AccountStatus;
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

/**
 * UUID primary key (not auto-increment) and money stored as bigint minor
 * units — same conventions as the funds-transfer schema, applied here too.
 */
@Entity
@Table(
        name = "account",
        indexes = {
                @Index(name = "idx_account_cif", columnList = "cif"),
                @Index(name = "idx_account_status", columnList = "status"),
                @Index(name = "idx_account_number", columnList = "account_number", unique = true)
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 30)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String cif;

    @Column(name = "keycloak_user_id", nullable = false)
    private UUID keycloakUserId;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType; // SAVINGS, CURRENT

    @Column(name = "balance_minor_units", nullable = false)
    private BigInteger balanceMinorUnits; // paise, never decimal/float

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus status;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode; // tenant discriminator

    @Version
    private Long version; // ReconciliationJob and a live balance-affecting write can race on this row

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
