package com.bank.termdeposit.config;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    /** Cheap liveness signal beyond the defaults — extend with real dependency checks (CBS, RabbitMQ) as needed. */
    @Bean
    public HealthIndicator termDepositServiceHealthIndicator() {
        return () -> Health.up().withDetail("service", "term-deposit-service").build();
    }
}
