package com.bank.ft.client.feign;

import com.bank.ft.client.config.FeignClientConfig;
import com.bank.ft.client.fallback.CbsFeignClientFallback;
import java.math.BigInteger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** Core Banking System debit leg of a transfer. No outbound call without a fallback and Resilience4j policy. */
@FeignClient(
        name = "cbs-service",
        url = "${clients.cbs.base-url}",
        configuration = FeignClientConfig.class,
        fallback = CbsFeignClientFallback.class
)
public interface CbsFeignClient {

    @PostMapping("/internal/v1/accounts/{accountNumber}/debit")
    DebitResult debit(@PathVariable("accountNumber") String accountNumber, BigInteger amountMinorUnits);

    record DebitResult(boolean success, String cbsReferenceNumber, String failureReason) {
    }
}
