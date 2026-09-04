package com.bank.termdeposit.client.feign;

import com.bank.termdeposit.client.config.FeignClientConfig;
import com.bank.termdeposit.client.config.InternalServiceSigningConfig;
import com.bank.termdeposit.client.fallback.NotificationFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Only ever called from {@code messaging/outbox} relay processing — never
 * synchronously from a controller/service critical path. Targets a
 * NestJS-side internal endpoint, so checksum signing is wired in via
 * {@link InternalServiceSigningConfig} (unlike CbsFeignClient).
 */
@FeignClient(
        name = "notification-service",
        url = "${clients.notification.base-url}",
        configuration = {FeignClientConfig.class, InternalServiceSigningConfig.class},
        fallback = NotificationFeignClientFallback.class
)
public interface NotificationFeignClient {

    @PostMapping("/internal/v1/notifications")
    void send(@RequestBody NotificationRequest request);

    record NotificationRequest(String cif, String channel, String template, String payloadJson) {
    }
}
