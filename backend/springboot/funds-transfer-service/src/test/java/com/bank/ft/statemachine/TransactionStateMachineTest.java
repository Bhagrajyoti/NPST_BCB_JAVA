package com.bank.ft.statemachine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.ft.common.exception.IllegalTransitionException;
import com.bank.ft.domain.statemachine.TransactionStateMachine;
import com.bank.ft.domain.statemachine.TransactionStatus;
import org.junit.jupiter.api.Test;

class TransactionStateMachineTest {

    private final TransactionStateMachine stateMachine = new TransactionStateMachine();

    @Test
    void allowsTheHappyPathSequence() {
        assertThatCode(() -> {
            stateMachine.assertTransitionAllowed(TransactionStatus.INITIATED, TransactionStatus.PENDING);
            stateMachine.assertTransitionAllowed(TransactionStatus.PENDING, TransactionStatus.PROCESSING);
            stateMachine.assertTransitionAllowed(TransactionStatus.PROCESSING, TransactionStatus.SUCCESS);
        }).doesNotThrowAnyException();
    }

    @Test
    void allowsReversalOnlyFromSuccess() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(TransactionStatus.SUCCESS, TransactionStatus.REVERSED))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsReversalFromAnyOtherState() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(TransactionStatus.FAILED, TransactionStatus.REVERSED))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void rejectsSkippingDirectlyFromInitiatedToSuccess() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(TransactionStatus.INITIATED, TransactionStatus.SUCCESS))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void rejectsAnyTransitionOutOfATerminalReversedState() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(TransactionStatus.REVERSED, TransactionStatus.SUCCESS))
                .isInstanceOf(IllegalTransitionException.class);
    }
}
