package com.bank.loan.common.security;

import com.bank.loan.common.exception.IdorDeniedException;
import com.bank.loan.common.security.keycloak.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Checks the resource owner (CIF) against the authenticated principal before
 * a service method touches the record. Call this explicitly wherever a
 * path/body parameter identifies a resource that belongs to a specific CIF.
 */
@Component
@RequiredArgsConstructor
public class IdorGuard {

    private final CurrentUserResolver currentUserResolver;

    public void assertOwnedByCurrentUser(String resourceCif) {
        String actingCif = currentUserResolver.currentCif();
        if (actingCif == null || !actingCif.equals(resourceCif)) {
            throw new IdorDeniedException("Authenticated principal does not own the requested resource");
        }
    }
}
