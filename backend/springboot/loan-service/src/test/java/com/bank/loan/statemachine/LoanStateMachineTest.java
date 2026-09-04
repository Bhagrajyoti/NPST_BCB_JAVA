package com.bank.loan.statemachine;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.loan.common.exception.IllegalTransitionException;
import com.bank.loan.domain.statemachine.LoanStateMachine;
import com.bank.loan.domain.statemachine.LoanStatus;
import org.junit.jupiter.api.Test;

class LoanStateMachineTest {

    private final LoanStateMachine stateMachine = new LoanStateMachine();

    @Test
    void allowsTheHappyPathSequence() {
        assertThatCode(() -> {
            stateMachine.assertTransitionAllowed(LoanStatus.APPLIED, LoanStatus.UNDER_REVIEW);
            stateMachine.assertTransitionAllowed(LoanStatus.UNDER_REVIEW, LoanStatus.APPROVED);
            stateMachine.assertTransitionAllowed(LoanStatus.APPROVED, LoanStatus.DISBURSED);
            stateMachine.assertTransitionAllowed(LoanStatus.DISBURSED, LoanStatus.ACTIVE);
            stateMachine.assertTransitionAllowed(LoanStatus.ACTIVE, LoanStatus.CLOSED);
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsApprovingAnAlreadyRejectedApplication() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(LoanStatus.REJECTED, LoanStatus.APPROVED))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void rejectsSkippingReviewDirectlyToApproved() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(LoanStatus.APPLIED, LoanStatus.APPROVED))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void allowsRecoveringAnActiveLoanFromDefault() {
        assertThatCode(() -> stateMachine.assertTransitionAllowed(LoanStatus.DEFAULTED, LoanStatus.ACTIVE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDisbursingAClosedLoan() {
        assertThatThrownBy(() -> stateMachine.assertTransitionAllowed(LoanStatus.CLOSED, LoanStatus.DISBURSED))
                .isInstanceOf(IllegalTransitionException.class);
    }
}
