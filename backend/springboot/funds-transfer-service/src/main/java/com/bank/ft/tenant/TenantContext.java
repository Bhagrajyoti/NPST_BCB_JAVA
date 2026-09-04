package com.bank.ft.tenant;

/**
 * Thread-local holder for the resolved bank/tenant code. Populated by
 * {@link TenantResolverFilter} at the top of the filter chain, cleared at
 * the end of every request.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String bankCode) {
        CURRENT_TENANT.set(bankCode);
    }

    public static String get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
