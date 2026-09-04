package com.bank.ft.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Micrometer + OpenTelemetry wiring shared by every controller/service via {@code @Timed}. */
@Configuration
public class ObservabilityConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public MeterFilter commonTagsFilter() {
        return MeterFilter.commonTags(List.of(Tag.of("service", "funds-transfer-service")));
    }
}
