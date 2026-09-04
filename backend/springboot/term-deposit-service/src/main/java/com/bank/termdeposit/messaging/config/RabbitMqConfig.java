package com.bank.termdeposit.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String TERM_DEPOSIT_EVENTS_EXCHANGE = "term-deposit.events";
    public static final String AUDIT_EVENTS_QUEUE = "term-deposit.audit-events";
    public static final String NOTIFICATION_EVENTS_QUEUE = "term-deposit.notification-events";

    @Bean
    public TopicExchange termDepositEventsExchange() {
        return new TopicExchange(TERM_DEPOSIT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue auditEventsQueue() {
        return new Queue(AUDIT_EVENTS_QUEUE, true);
    }

    @Bean
    public Queue notificationEventsQueue() {
        return new Queue(NOTIFICATION_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding auditEventsBinding(Queue auditEventsQueue, TopicExchange termDepositEventsExchange) {
        return BindingBuilder.bind(auditEventsQueue).to(termDepositEventsExchange).with("audit.*");
    }

    @Bean
    public Binding notificationEventsBinding(Queue notificationEventsQueue, TopicExchange termDepositEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(termDepositEventsExchange).with("notification.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
