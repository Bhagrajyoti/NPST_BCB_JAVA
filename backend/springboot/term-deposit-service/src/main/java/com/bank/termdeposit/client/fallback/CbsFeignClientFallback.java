package com.bank.termdeposit.client.fallback;

import com.bank.termdeposit.client.feign.CbsFeignClient;
import com.bank.termdeposit.common.exception.CbsUnavailableException;
import java.math.BigInteger;
import org.springframework.stereotype.Component;

@Component
public class CbsFeignClientFallback implements CbsFeignClient {

    @Override
    public CbsResult debit(String accountNumber, BigInteger amountMinorUnits) {
        throw new CbsUnavailableException("CBS is currently unavailable, cannot debit account " + accountNumber);
    }

    @Override
    public CbsResult credit(String accountNumber, BigInteger amountMinorUnits) {
        throw new CbsUnavailableException("CBS is currently unavailable, cannot credit account " + accountNumber);
    }
}
