package com.bank.termdeposit.domain.statemachine;

import com.bank.termdeposit.common.exception.IllegalTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TermDepositStateMachine {

    private static final Map<TermDepositStatus, Set<TermDepositStatus>> TRANSITIONS = new EnumMap<>(TermDepositStatus.class);

    static {
        TRANSITIONS.put(TermDepositStatus.PENDING, EnumSet.of(TermDepositStatus.ACTIVE, TermDepositStatus.CLOSED));
        TRANSITIONS.put(TermDepositStatus.ACTIVE, EnumSet.of(TermDepositStatus.MATURED, TermDepositStatus.PREMATURELY_CLOSED));
        TRANSITIONS.put(TermDepositStatus.MATURED, EnumSet.of(TermDepositStatus.CLOSED));
        TRANSITIONS.put(TermDepositStatus.PREMATURELY_CLOSED, EnumSet.noneOf(TermDepositStatus.class));
        TRANSITIONS.put(TermDepositStatus.CLOSED, EnumSet.noneOf(TermDepositStatus.class));
    }

    public void assertTransitionAllowed(TermDepositStatus from, TermDepositStatus to) {
        if (from == to) {
            return;
        }
        Set<TermDepositStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalTransitionException("Illegal term deposit status transition: " + from + " -> " + to);
        }
    }
}
