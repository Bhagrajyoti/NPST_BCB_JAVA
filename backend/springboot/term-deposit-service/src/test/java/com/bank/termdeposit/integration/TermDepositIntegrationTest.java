package com.bank.termdeposit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.termdeposit.domain.entity.TermDeposit;
import com.bank.termdeposit.domain.repository.TermDepositRepository;
import com.bank.termdeposit.domain.statemachine.TermDepositStatus;
import java.math.BigInteger;
import java.time.LocalDate;
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

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class TermDepositIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("term_deposit_service")
            .withUsername("term_deposit_service")
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
    private TermDepositRepository termDepositRepository;

    @Test
    void persistsAndReloadsATermDepositWithOptimisticLocking() {
        TermDeposit deposit = TermDeposit.builder()
                .cif("9999999999")
                .keycloakUserId(UUID.randomUUID())
                .sourceAccountNumber("1234567890123")
                .principalMinorUnits(BigInteger.valueOf(1_000_000))
                .interestRateBps(650)
                .tenureMonths(12)
                .maturityDate(LocalDate.now().plusMonths(12))
                .maturityAmountMinorUnits(BigInteger.valueOf(1_065_000))
                .currency("INR")
                .status(TermDepositStatus.ACTIVE)
                .bankCode("default-bank")
                .build();

        TermDeposit saved = termDepositRepository.saveAndFlush(deposit);
        assertThat(saved.getVersion()).isZero();

        TermDeposit reloaded = termDepositRepository.findById(saved.getId()).orElseThrow();
        reloaded.setStatus(TermDepositStatus.MATURED);
        TermDeposit updated = termDepositRepository.saveAndFlush(reloaded);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo(TermDepositStatus.MATURED);
    }
}
