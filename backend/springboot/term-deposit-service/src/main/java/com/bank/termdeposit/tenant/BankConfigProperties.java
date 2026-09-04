package com.bank.termdeposit.tenant;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Per-tenant config: CIF format, limits, interest-rate cards. One entry
 * exists today (the single live bank) but this stays wired from day one so
 * the next bank's onboarding is a config change, not a fork.
 */
@Component
@ConfigurationProperties(prefix = "bank")
@Getter
@Setter
public class BankConfigProperties {

    private Map<String, Tenant> tenants = Map.of();

    public Tenant forCode(String bankCode) {
        Tenant tenant = tenants.get(bankCode);
        if (tenant == null) {
            throw new IllegalStateException("No bank config registered for tenant: " + bankCode);
        }
        return tenant;
    }

    @Getter
    @Setter
    public static class Tenant {
        private String cifRegex;
        private String defaultCurrency = "INR";
        private int defaultInterestRateBps = 650;
    }
}
