package com.bank.ft.client.config;

import com.bank.ft.common.security.checksum.ChecksumProperties;
import com.bank.ft.common.security.checksum.ChecksumUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

/**
 * Signs outbound calls to {@code /internal/**} routes on the NestJS side
 * (Auth/Bill Payment/Admin modulith). Deliberately NOT a {@code @Component}
 * — a globally-registered {@code RequestInterceptor} bean is applied by
 * Spring Cloud OpenFeign to every Feign client in the service, including
 * ones (CBS, Switch) that were never part of the checksum contract. Wire
 * this in only via the {@code configuration} attribute of the specific
 * {@code @FeignClient}s that call NestJS-side internal endpoints — see
 * {@link InternalServiceSigningConfig}.
 */
@RequiredArgsConstructor
public class ChecksumRequestInterceptor implements RequestInterceptor {

    private static final String THIS_SERVICE_NAME = "funds-transfer-service";

    private final ChecksumProperties checksumProperties;
    private final ChecksumUtil checksumUtil;

    @Override
    public void apply(RequestTemplate template) {
        String targetService = template.feignTarget().name();
        String secret = checksumProperties.secretFor(targetService);
        String timestamp = Instant.now().toString();
        String payload = template.body() != null ? new String(template.body(), StandardCharsets.UTF_8) : "";
        String checksum = checksumUtil.sign(payload, timestamp, secret);

        template.header(checksumProperties.getCallerHeaderName(), THIS_SERVICE_NAME);
        template.header(checksumProperties.getTimestampHeaderName(), timestamp);
        template.header(checksumProperties.getHeaderName(), checksum);
    }
}
