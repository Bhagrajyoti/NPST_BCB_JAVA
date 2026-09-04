package com.bank.loan.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.loan.common.exception.IdorDeniedException;
import com.bank.loan.common.security.IdorGuard;
import com.bank.loan.common.security.keycloak.CurrentUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Simulates a caller whose JWT identifies them as CIF "1111111111" trying
 * to read a resource owned by a different CIF — the exact shape of an IDOR
 * attempt against an account-detail endpoint.
 */
@ExtendWith(MockitoExtension.class)
class IdorAttemptTest {

    @Mock
    private CurrentUserResolver currentUserResolver;

    private IdorGuard idorGuard;

    @BeforeEach
    void setUp() {
        idorGuard = new IdorGuard(currentUserResolver);
    }

    @Test
    void deniesAccessWhenResourceCifDoesNotMatchAuthenticatedCif() {
        when(currentUserResolver.currentCif()).thenReturn("1111111111");

        assertThatThrownBy(() -> idorGuard.assertOwnedByCurrentUser("2222222222"))
                .isInstanceOf(IdorDeniedException.class);
    }

    @Test
    void allowsAccessWhenResourceCifMatchesAuthenticatedCif() {
        when(currentUserResolver.currentCif()).thenReturn("1111111111");

        assertThatCode(() -> idorGuard.assertOwnedByCurrentUser("1111111111"))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesAccessWhenAuthenticatedCifIsMissing() {
        when(currentUserResolver.currentCif()).thenReturn(null);

        assertThatThrownBy(() -> idorGuard.assertOwnedByCurrentUser("1111111111"))
                .isInstanceOf(IdorDeniedException.class);
    }
}
