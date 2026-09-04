package com.bank.loan.client.feign;

import com.bank.loan.client.config.FeignClientConfig;
import com.bank.loan.client.fallback.CreditBureauFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** External credit bureau lookup used during underwriting (UNDER_REVIEW). */
@FeignClient(
        name = "credit-bureau-service",
        url = "${clients.credit-bureau.base-url}",
        configuration = FeignClientConfig.class,
        fallback = CreditBureauFeignClientFallback.class
)
public interface CreditBureauFeignClient {

    @GetMapping("/v1/credit-score/{cif}")
    CreditScoreResult fetchCreditScore(@PathVariable("cif") String cif);

    record CreditScoreResult(boolean available, Integer score) {
    }
}
