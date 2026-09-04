package com.bank.loan.messaging.config;

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

    public static final String LOAN_EVENTS_EXCHANGE = "loan.events";
    public static final String AUDIT_EVENTS_QUEUE = "loan.audit-events";
    public static final String NOTIFICATION_EVENTS_QUEUE = "loan.notification-events";

    @Bean
    public TopicExchange loanEventsExchange() {
        return new TopicExchange(LOAN_EVENTS_EXCHANGE, true, false);
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
    public Binding auditEventsBinding(Queue auditEventsQueue, TopicExchange loanEventsExchange) {
        return BindingBuilder.bind(auditEventsQueue).to(loanEventsExchange).with("audit.*");
    }

    @Bean
    public Binding notificationEventsBinding(Queue notificationEventsQueue, TopicExchange loanEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(loanEventsExchange).with("notification.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
