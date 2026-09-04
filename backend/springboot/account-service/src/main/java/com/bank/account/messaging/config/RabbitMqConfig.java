package com.bank.account.messaging.config;

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

    public static final String ACCOUNT_EVENTS_EXCHANGE = "account.events";
    public static final String AUDIT_EVENTS_QUEUE = "account.audit-events";
    public static final String NOTIFICATION_EVENTS_QUEUE = "account.notification-events";

    @Bean
    public TopicExchange accountEventsExchange() {
        return new TopicExchange(ACCOUNT_EVENTS_EXCHANGE, true, false);
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
    public Binding auditEventsBinding(Queue auditEventsQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(auditEventsQueue).to(accountEventsExchange).with("audit.*");
    }

    @Bean
    public Binding notificationEventsBinding(Queue notificationEventsQueue, TopicExchange accountEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(accountEventsExchange).with("notification.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
