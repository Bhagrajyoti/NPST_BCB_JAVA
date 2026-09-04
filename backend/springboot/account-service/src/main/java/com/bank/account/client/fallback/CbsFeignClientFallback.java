package com.bank.account.client.fallback;

import com.bank.account.client.feign.CbsFeignClient;
import com.bank.account.common.exception.CbsUnavailableException;
import java.math.BigInteger;
import org.springframework.stereotype.Component;

/** Every Feign client has exactly one fallback — no exceptions for "just this one internal call." */
@Component
public class CbsFeignClientFallback implements CbsFeignClient {

    @Override
    public BigInteger fetchBalanceMinorUnits(String accountNumber) {
        throw new CbsUnavailableException("CBS is currently unavailable for account " + accountNumber);
    }
}
