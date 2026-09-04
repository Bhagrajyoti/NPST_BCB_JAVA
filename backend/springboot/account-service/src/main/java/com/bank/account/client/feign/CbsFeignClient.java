package com.bank.account.client.feign;

import com.bank.account.client.config.FeignClientConfig;
import com.bank.account.client.fallback.CbsFeignClientFallback;
import java.math.BigInteger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Core Banking System integration. No outbound call without a fallback and Resilience4j policy. */
@FeignClient(
        name = "cbs-service",
        url = "${clients.cbs.base-url}",
        configuration = FeignClientConfig.class,
        fallback = CbsFeignClientFallback.class
)
public interface CbsFeignClient {

    @GetMapping("/internal/v1/accounts/{accountNumber}/balance")
    BigInteger fetchBalanceMinorUnits(@PathVariable("accountNumber") String accountNumber);
}
