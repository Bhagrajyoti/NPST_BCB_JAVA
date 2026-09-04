package com.bank.loan.domain.statemachine;

import com.bank.loan.common.exception.IllegalTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Loans is a Complex-rated domain — every transition here is explicit and
 * anything not listed throws, including reviewer-vs-disbursement-job races
 * on the same application.
 */
@Component
public class LoanStateMachine {

    private static final Map<LoanStatus, Set<LoanStatus>> TRANSITIONS = new EnumMap<>(LoanStatus.class);

    static {
        TRANSITIONS.put(LoanStatus.APPLIED, EnumSet.of(LoanStatus.UNDER_REVIEW, LoanStatus.REJECTED));
        TRANSITIONS.put(LoanStatus.UNDER_REVIEW, EnumSet.of(LoanStatus.APPROVED, LoanStatus.REJECTED));
        TRANSITIONS.put(LoanStatus.APPROVED, EnumSet.of(LoanStatus.DISBURSED, LoanStatus.REJECTED));
        TRANSITIONS.put(LoanStatus.REJECTED, EnumSet.noneOf(LoanStatus.class));
        TRANSITIONS.put(LoanStatus.DISBURSED, EnumSet.of(LoanStatus.ACTIVE));
        TRANSITIONS.put(LoanStatus.ACTIVE, EnumSet.of(LoanStatus.CLOSED, LoanStatus.FORECLOSED, LoanStatus.DEFAULTED));
        TRANSITIONS.put(LoanStatus.CLOSED, EnumSet.noneOf(LoanStatus.class));
        TRANSITIONS.put(LoanStatus.FORECLOSED, EnumSet.noneOf(LoanStatus.class));
        TRANSITIONS.put(LoanStatus.DEFAULTED, EnumSet.of(LoanStatus.ACTIVE, LoanStatus.CLOSED));
    }

    public void assertTransitionAllowed(LoanStatus from, LoanStatus to) {
        if (from == to) {
            return;
        }
        Set<LoanStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalTransitionException("Illegal loan status transition: " + from + " -> " + to);
        }
    }
}
