package com.bank.ft.client.fallback;

import com.bank.ft.client.feign.SwitchFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Every Feign client has exactly one fallback — no exceptions for "just this one internal call." */
@Slf4j
@Component
public class SwitchFeignClientFallback implements SwitchFeignClient {

    @Override
    public SwitchTransferResult submitTransfer(SwitchTransferRequest request) {
        log.error("switch-service unavailable for transfer {}", request.transactionReference());
        return new SwitchTransferResult(false, null, "SWITCH_UNAVAILABLE");
    }
}
