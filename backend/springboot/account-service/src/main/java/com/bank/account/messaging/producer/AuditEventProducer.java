package com.bank.account.messaging.producer;

import com.bank.account.messaging.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Domain event producer for audit trail entries. Called only from the
 * outbox relay ({@code OutboxRelayScheduler}) — never synchronously from
 * inside a transaction's critical path.
 */
@Component
@RequiredArgsConstructor
public class AuditEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.ACCOUNT_EVENTS_EXCHANGE, routingKey, payload);
    }
}
