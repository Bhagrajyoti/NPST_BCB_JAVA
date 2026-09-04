package com.bank.account.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.account.common.security.checksum.ChecksumUtil;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Verifies the primitive the whole checksum contract rests on: a tampered
 * payload or a checksum signed with the wrong secret never validates.
 * {@code ChecksumValidationFilter} itself is exercised via the
 * "hits /internal/** without a checksum header -> fails" case in the DoD
 * checklist, best proven with a full MockMvc/WebTestClient slice once the
 * NestJS-side header contract (architecture doc §6) is confirmed.
 */
class ChecksumTamperingTest {

    private final ChecksumUtil checksumUtil = new ChecksumUtil();
    private static final String SECRET = "shared-secret-for-account-service-and-notification-service";

    @Test
    void validSignatureMatchesForUntamperedPayload() {
        String timestamp = Instant.now().toString();
        String payload = "{\"accountId\":\"abc-123\"}";
        String checksum = checksumUtil.sign(payload, timestamp, SECRET);

        assertThat(checksumUtil.matches(payload, timestamp, SECRET, checksum)).isTrue();
    }

    @Test
    void tamperedPayloadFailsChecksumValidation() {
        String timestamp = Instant.now().toString();
        String originalPayload = "{\"amountMinorUnits\":10000}";
        String checksum = checksumUtil.sign(originalPayload, timestamp, SECRET);

        String tamperedPayload = "{\"amountMinorUnits\":9999999}";

        assertThat(checksumUtil.matches(tamperedPayload, timestamp, SECRET, checksum)).isFalse();
    }

    @Test
    void wrongSecretFailsChecksumValidation() {
        String timestamp = Instant.now().toString();
        String payload = "{\"accountId\":\"abc-123\"}";
        String checksum = checksumUtil.sign(payload, timestamp, SECRET);

        assertThat(checksumUtil.matches(payload, timestamp, "a-completely-different-secret", checksum)).isFalse();
    }

    @Test
    void tamperedTimestampFailsChecksumValidation() {
        String payload = "{\"accountId\":\"abc-123\"}";
        String checksum = checksumUtil.sign(payload, Instant.now().toString(), SECRET);

        String laterTimestamp = Instant.now().plusSeconds(3600).toString();

        assertThat(checksumUtil.matches(payload, laterTimestamp, SECRET, checksum)).isFalse();
    }
}
