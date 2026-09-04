package com.bank.termdeposit.client.feign;

import com.bank.termdeposit.client.config.FeignClientConfig;
import com.bank.termdeposit.client.fallback.CbsFeignClientFallback;
import java.math.BigInteger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** Core Banking System integration: fund a new deposit, pay out on maturity/premature closure. */
@FeignClient(
        name = "cbs-service",
        url = "${clients.cbs.base-url}",
        configuration = FeignClientConfig.class,
        fallback = CbsFeignClientFallback.class
)
public interface CbsFeignClient {

    @PostMapping("/internal/v1/accounts/{accountNumber}/debit")
    CbsResult debit(@PathVariable("accountNumber") String accountNumber, BigInteger amountMinorUnits);

    @PostMapping("/internal/v1/accounts/{accountNumber}/credit")
    CbsResult credit(@PathVariable("accountNumber") String accountNumber, BigInteger amountMinorUnits);

    record CbsResult(boolean success, String cbsReferenceNumber, String failureReason) {
    }
}
