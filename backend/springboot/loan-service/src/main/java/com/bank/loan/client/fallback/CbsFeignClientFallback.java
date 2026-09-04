package com.bank.loan.client.fallback;

import com.bank.loan.client.feign.CbsFeignClient;
import com.bank.loan.common.exception.CbsUnavailableException;
import java.math.BigInteger;
import org.springframework.stereotype.Component;

@Component
public class CbsFeignClientFallback implements CbsFeignClient {

    @Override
    public CbsResult disburse(String accountNumber, BigInteger amountMinorUnits) {
        throw new CbsUnavailableException("CBS is currently unavailable, cannot disburse to account " + accountNumber);
    }

    @Override
    public CbsResult collectEmi(String accountNumber, BigInteger amountMinorUnits) {
        throw new CbsUnavailableException("CBS is currently unavailable, cannot collect EMI from account " + accountNumber);
    }
}
