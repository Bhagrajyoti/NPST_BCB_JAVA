package com.bank.loan.client.feign;

import com.bank.loan.client.config.FeignClientConfig;
import com.bank.loan.client.fallback.CbsFeignClientFallback;
import java.math.BigInteger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** Core Banking System integration: disburse the loan principal, collect EMIs. */
@FeignClient(
        name = "cbs-service",
        url = "${clients.cbs.base-url}",
        configuration = FeignClientConfig.class,
        fallback = CbsFeignClientFallback.class
)
public interface CbsFeignClient {

    @PostMapping("/internal/v1/accounts/{accountNumber}/credit")
    CbsResult disburse(@PathVariable("accountNumber") String accountNumber, BigInteger amountMinorUnits);

    @PostMapping("/internal/v1/accounts/{accountNumber}/debit")
    CbsResult collectEmi(@PathVariable("accountNumber") String accountNumber, BigInteger amountMinorUnits);

    record CbsResult(boolean success, String cbsReferenceNumber, String failureReason) {
    }
}
