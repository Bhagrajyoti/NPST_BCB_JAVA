package com.bank.account.common.security.checksum;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** HMAC-SHA256(payload + timestamp, secret) — used both to sign outbound internal calls and validate inbound ones. */
@Component
public class ChecksumUtil {

    private static final String ALGORITHM = "HmacSHA256";

    public String sign(String payload, String timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] signed = mac.doFinal((payload + timestamp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signed);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to compute checksum", e);
        }
    }

    public boolean matches(String payload, String timestamp, String secret, String candidateChecksum) {
        String expected = sign(payload, timestamp, secret);
        return constantTimeEquals(expected, candidateChecksum);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
