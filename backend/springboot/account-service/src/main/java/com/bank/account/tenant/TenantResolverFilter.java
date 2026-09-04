package com.bank.account.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the bank/tenant from a request header today. Rollout mechanism
 * (header vs. subdomain vs. JWT claim) is an open item — see the
 * architecture doc §6 — but the resolution point stays this one filter
 * regardless of which mechanism is finalized.
 */
@Component
public class TenantResolverFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Bank-Code";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String bankCode = request.getHeader(TENANT_HEADER);
            if (bankCode != null && !bankCode.isBlank()) {
                TenantContext.set(bankCode);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
