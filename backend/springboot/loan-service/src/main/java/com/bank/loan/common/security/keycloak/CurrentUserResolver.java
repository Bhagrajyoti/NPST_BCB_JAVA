package com.bank.loan.common.security.keycloak;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Pulls {@code sub}/CIF/roles off the already-validated JWT. Controllers
 * must resolve the acting user through here — never trust a client-supplied
 * CIF or user id in the request body/params for anything security-relevant.
 */
@Component
public class CurrentUserResolver {

    private static final String CIF_CLAIM = "cif";

    public UUID currentKeycloakUserId() {
        return UUID.fromString(currentJwt().getSubject());
    }

    public String currentCif() {
        return currentJwt().getClaimAsString(CIF_CLAIM);
    }

    public Jwt currentJwt() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("No authenticated JWT principal in the current security context");
        }
        return token.getToken();
    }
}
