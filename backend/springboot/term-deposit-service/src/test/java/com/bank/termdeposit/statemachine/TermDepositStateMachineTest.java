package com.bank.termdeposit.statemachine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.termdeposit.common.exception.IllegalTransitionException;
import com.bank.termdeposit.domain.statemachine.TermDepositStateMachine;
import com.bank.termdeposit.domain.statemachine.TermDepositStatus;
import org.junit.jupiter.api.Test;

class TermDepositStateMachineTest {

    private final TermDepositStateMachine stateMachine = new TermDepositStateMachine();

    @Test
    void allowsPendingToActiveToMaturedToClosed() {
        assertThatCode(() -> {
            stateMachine.assertTransitionAllowed(TermDepositStatus.PENDING, TermDepositStatus.ACTIVE);
            stateMachine.assertTransitionAllowed(TermDepositStatus.ACTIVE, TermDepositStatus.MATURED);
            stateMachine.assertTransitionAllowed(TermDepositStatus.MATURED, TermDepositStatus.CLOSED);
        }).doesNotThrowAnyException();
    }

    @Test
    void allowsActiveToPrematurelyClosed() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(TermDepositStatus.ACTIVE, TermDepositStatus.PREMATURELY_CLOSED))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsReactivatingAPrematurelyClosedDeposit() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(
                TermDepositStatus.PREMATURELY_CLOSED, TermDepositStatus.ACTIVE))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void rejectsSkippingActiveDirectlyToMatured() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(TermDepositStatus.PENDING, TermDepositStatus.MATURED))
                .isInstanceOf(IllegalTransitionException.class);
    }
}
