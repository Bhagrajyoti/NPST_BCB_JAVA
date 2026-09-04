package com.bank.account.messaging.outbox;

import com.bank.account.messaging.producer.AuditEventProducer;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anything non-blocking goes through here. Polls PENDING outbox rows and
 * relays them — the write that created the row already committed, so a
 * relay failure here only delays the side effect, it never rolls back the
 * business transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final AuditEventProducer auditEventProducer;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:5000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            relay(event);
        }
    }

    private void relay(OutboxEvent event) {
        try {
            String routingKey = routingKeyFor(event.getEventType());
            auditEventProducer.publish(routingKey, event.getPayload());
            event.setStatus(OutboxStatus.SENT);
            event.setSentAt(Instant.now());
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            if (event.getRetryCount() >= MAX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox event {} exceeded max retries, marking FAILED", event.getId(), e);
            } else {
                log.warn("Outbox event {} relay failed, will retry (attempt {})", event.getId(), event.getRetryCount(), e);
            }
        } finally {
            outboxEventRepository.save(event);
        }
    }

    private String routingKeyFor(String eventType) {
        return eventType.toLowerCase().startsWith("notification") ? "notification." + eventType : "audit." + eventType;
    }
}
