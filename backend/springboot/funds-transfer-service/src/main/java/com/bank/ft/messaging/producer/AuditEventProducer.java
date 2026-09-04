package com.bank.ft.messaging.producer;

import com.bank.ft.messaging.config.RabbitMqConfig;
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
        rabbitTemplate.convertAndSend(RabbitMqConfig.FT_EVENTS_EXCHANGE, routingKey, payload);
    }
}
