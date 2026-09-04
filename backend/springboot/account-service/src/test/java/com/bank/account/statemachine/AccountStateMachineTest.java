package com.bank.account.statemachine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.account.common.exception.IllegalTransitionException;
import com.bank.account.domain.statemachine.AccountStateMachine;
import com.bank.account.domain.statemachine.AccountStatus;
import org.junit.jupiter.api.Test;

class AccountStateMachineTest {

    private final AccountStateMachine stateMachine = new AccountStateMachine();

    @Test
    void allowsPendingActivationToActive() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(
                AccountStatus.PENDING_ACTIVATION, AccountStatus.ACTIVE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsClosedToActive() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(
                AccountStatus.CLOSED, AccountStatus.ACTIVE))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void rejectsSkippingActivationDirectlyToFrozen() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(
                AccountStatus.PENDING_ACTIVATION, AccountStatus.FROZEN))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void sameStateIsAlwaysANoOp() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(AccountStatus.ACTIVE, AccountStatus.ACTIVE))
                .doesNotThrowAnyException();
    }
}
