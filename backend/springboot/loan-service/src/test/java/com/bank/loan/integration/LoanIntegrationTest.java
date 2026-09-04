package com.bank.loan.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.loan.domain.entity.Loan;
import com.bank.loan.domain.repository.LoanRepository;
import com.bank.loan.domain.statemachine.LoanStatus;
import java.math.BigInteger;
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
class LoanIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("loan_service")
            .withUsername("loan_service")
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
    private LoanRepository loanRepository;

    @Test
    void persistsAndReloadsALoanWithOptimisticLocking() {
        Loan loan = Loan.builder()
                .cif("9999999999")
                .keycloakUserId(UUID.randomUUID())
                .loanType("PERSONAL")
                .disbursementAccountNumber("1234567890123")
                .principalMinorUnits(BigInteger.valueOf(500_000))
                .interestRateBps(1200)
                .tenureMonths(24)
                .currency("INR")
                .status(LoanStatus.APPLIED)
                .bankCode("default-bank")
                .build();

        Loan saved = loanRepository.saveAndFlush(loan);
        assertThat(saved.getVersion()).isZero();

        Loan reloaded = loanRepository.findById(saved.getId()).orElseThrow();
        reloaded.setStatus(LoanStatus.UNDER_REVIEW);
        Loan updated = loanRepository.saveAndFlush(reloaded);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo(LoanStatus.UNDER_REVIEW);
    }
}
