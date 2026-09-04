package com.bank.account.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.domain.entity.Account;
import com.bank.account.domain.repository.AccountRepository;
import com.bank.account.domain.statemachine.AccountStatus;
import java.math.BigInteger;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Real Postgres, real RabbitMQ — verifies the entity actually round-trips through Flyway-managed schema. */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AccountIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("account_service")
            .withUsername("account_service")
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
    private AccountRepository accountRepository;

    @Test
    void persistsAndReloadsAnAccountWithOptimisticLocking() {
        Account account = Account.builder()
                .accountNumber("AC" + UUID.randomUUID())
                .cif("9999999999")
                .keycloakUserId(UUID.randomUUID())
                .accountType("SAVINGS")
                .balanceMinorUnits(BigInteger.ZERO)
                .currency("INR")
                .status(AccountStatus.PENDING_ACTIVATION)
                .bankCode("default-bank")
                .build();

        Account saved = accountRepository.saveAndFlush(account);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();

        Account reloaded = accountRepository.findById(saved.getId()).orElseThrow();
        reloaded.setStatus(AccountStatus.ACTIVE);
        Account updated = accountRepository.saveAndFlush(reloaded);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
