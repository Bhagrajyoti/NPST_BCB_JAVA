package com.bank.account.client.fallback;

import com.bank.account.client.feign.NotificationFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Notification failures never fail the caller — the outbox relay retries
 * the event later, it does not propagate to whatever triggered the event.
 */
@Slf4j
@Component
public class NotificationFeignClientFallback implements NotificationFeignClient {

    @Override
    public void send(NotificationRequest request) {
        log.warn("notification-service unavailable, event will be retried by the outbox relay: cif={}, template={}",
                request.cif(), request.template());
    }
}
