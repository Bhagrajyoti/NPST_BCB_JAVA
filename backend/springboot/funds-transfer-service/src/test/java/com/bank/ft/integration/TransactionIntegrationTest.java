package com.bank.ft.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.ft.domain.entity.Transaction;
import com.bank.ft.domain.repository.TransactionRepository;
import com.bank.ft.domain.statemachine.TransactionStatus;
import com.bank.ft.domain.statemachine.TransferMode;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Real Postgres, real RabbitMQ — verifies the unique constraints (transaction_reference, idempotency_key) actually hold. */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("funds_transfer_service")
            .withUsername("funds_transfer_service")
            .withPassword("changeme");

    @Container
    static RabbitMQContainer rabbitMq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", () -> rabbitMq.getAmqpPort());
    }

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void persistsAndReloadsATransactionWithOptimisticLocking() {
        Transaction transaction = Transaction.builder()
                .transactionReference("TXN" + UUID.randomUUID())
                .idempotencyKey("idem-" + UUID.randomUUID())
                .initiatorCif("9999999999")
                .initiatorKeycloakUserId(UUID.randomUUID())
                .destinationAccountNumber("1234567890123")
                .destinationIfscCode("HDFC0001234")
                .amountMinorUnits(BigInteger.valueOf(50000))
                .currency("INR")
                .transferMode(TransferMode.IMPS)
                .status(TransactionStatus.INITIATED)
                .bankCode("default-bank")
                .initiatedAt(Instant.now())
                .build();

        Transaction saved = transactionRepository.saveAndFlush(transaction);
        assertThat(saved.getVersion()).isZero();

        Transaction reloaded = transactionRepository.findById(saved.getId()).orElseThrow();
        reloaded.setStatus(TransactionStatus.PENDING);
        Transaction updated = transactionRepository.saveAndFlush(reloaded);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }
}
