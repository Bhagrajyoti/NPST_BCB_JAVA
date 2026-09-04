package com.bank.loan.client.fallback;

import com.bank.loan.client.feign.CreditBureauFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Bureau unavailability never silently approves a loan — it just means no
 * score is available, and the caller (LoanServiceImpl) must route the
 * application to manual review rather than auto-decision it.
 */
@Slf4j
@Component
public class CreditBureauFeignClientFallback implements CreditBureauFeignClient {

    @Override
    public CreditScoreResult fetchCreditScore(String cif) {
        log.warn("credit-bureau-service unavailable for cif={}, routing to manual review", cif);
        return new CreditScoreResult(false, null);
    }
}
