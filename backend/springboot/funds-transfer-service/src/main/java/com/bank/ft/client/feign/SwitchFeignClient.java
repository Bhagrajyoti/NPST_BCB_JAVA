package com.bank.ft.client.feign;

import com.bank.ft.client.config.FeignClientConfig;
import com.bank.ft.client.fallback.SwitchFeignClientFallback;
import java.math.BigInteger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** NPCI switch integration for IMPS; NEFT/RTGS route through the same switch abstraction. */
@FeignClient(
        name = "switch-service",
        url = "${clients.switch.base-url}",
        configuration = FeignClientConfig.class,
        fallback = SwitchFeignClientFallback.class
)
public interface SwitchFeignClient {

    @PostMapping("/internal/v1/switch/transfers")
    SwitchTransferResult submitTransfer(@RequestBody SwitchTransferRequest request);

    record SwitchTransferRequest(
            String transactionReference,
            String sourceAccountNumber,
            String destinationAccountNumber,
            String destinationIfscCode,
            BigInteger amountMinorUnits,
            String transferMode
    ) {
    }

    record SwitchTransferResult(boolean accepted, String switchReferenceNumber, String failureReason) {
    }
}
