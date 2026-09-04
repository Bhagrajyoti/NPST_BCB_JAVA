package com.bank.ft.client.fallback;

import com.bank.ft.client.feign.CbsFeignClient;
import com.bank.ft.common.exception.CbsUnavailableException;
import java.math.BigInteger;
import org.springframework.stereotype.Component;

@Component
public class CbsFeignClientFallback implements CbsFeignClient {

    @Override
    public DebitResult debit(String accountNumber, BigInteger amountMinorUnits) {
        throw new CbsUnavailableException("CBS is currently unavailable, cannot debit account " + accountNumber);
    }
}
