package com.bank.account.common.security.checksum;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Checksum secrets are scoped per service-pair and sourced from vault (via
 * the config server / externalized property source) — never one global
 * shared secret across every inter-service call.
 *
 * application.yml supplies keys like:
 * checksum.secrets.nestjs-auth-service=${VAULT:CHECKSUM_SECRET_NESTJS_AUTH}
 * checksum.secrets.funds-transfer-service=${VAULT:CHECKSUM_SECRET_FT}
 */
@Component
@ConfigurationProperties(prefix = "checksum")
@Getter
@Setter
public class ChecksumProperties {

    private Map<String, String> secrets = Map.of();

    /** Header carrying the signed checksum on inbound/outbound internal calls. */
    private String headerName = "X-Checksum";

    /** Header identifying the calling service, used to select the per-pair secret. */
    private String callerHeaderName = "X-Caller-Service";

    /** Header carrying the signed timestamp, used to bound replay window. */
    private String timestampHeaderName = "X-Checksum-Timestamp";

    private long toleranceSeconds = 300;

    public String secretFor(String servicePairKey) {
        String secret = secrets.get(servicePairKey);
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("No checksum secret configured for service pair: " + servicePairKey);
        }
        return secret;
    }
}
