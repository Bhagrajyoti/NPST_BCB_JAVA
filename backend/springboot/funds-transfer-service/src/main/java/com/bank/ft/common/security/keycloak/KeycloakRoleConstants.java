package com.bank.ft.common.security.keycloak;

/**
 * Every role check in the codebase references a constant from here — never
 * a raw string in a {@code @PreAuthorize} expression.
 */
public final class KeycloakRoleConstants {

    private KeycloakRoleConstants() {
    }

    public static final String CUSTOMER = "CUSTOMER";
    public static final String OPS = "OPS";
    public static final String CHECKER = "CHECKER";
    public static final String ADMIN = "ADMIN";
}
