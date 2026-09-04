package com.bank.account.client.config;

import com.bank.account.common.security.checksum.ChecksumProperties;
import com.bank.account.common.security.checksum.ChecksumUtil;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Applied via {@code @FeignClient(configuration = ...)} only to clients
 * that call NestJS-side {@code /internal/**} endpoints — never registered
 * as a global bean. See {@link ChecksumRequestInterceptor}.
 */
public class InternalServiceSigningConfig {

    @Bean
    public RequestInterceptor checksumRequestInterceptor(
            ChecksumProperties checksumProperties, ChecksumUtil checksumUtil) {
        return new ChecksumRequestInterceptor(checksumProperties, checksumUtil);
    }
}
