package com.bank.loan.config;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    /** Cheap liveness signal beyond the defaults — extend with real dependency checks (CBS, RabbitMQ) as needed. */
    @Bean
    public HealthIndicator loanServiceHealthIndicator() {
        return () -> Health.up().withDetail("service", "loan-service").build();
    }
}
