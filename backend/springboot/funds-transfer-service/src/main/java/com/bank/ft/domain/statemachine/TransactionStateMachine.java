package com.bank.ft.domain.statemachine;

import com.bank.ft.common.exception.IllegalTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code INITIATED -> PENDING -> PROCESSING -> SUCCESS/FAILED}, and only
 * {@code SUCCESS -> REVERSED} is ever legal in reverse. Anything else
 * throws — this is what makes an illegal outcome of the
 * ReconciliationJob-vs-live-status-check race impossible to persist; the
 * {@code @Version} column on {@link com.bank.ft.domain.entity.Transaction}
 * is what makes the race itself detectable.
 */
@Component
public class TransactionStateMachine {

    private static final Map<TransactionStatus, Set<TransactionStatus>> TRANSITIONS = new EnumMap<>(TransactionStatus.class);

    static {
        TRANSITIONS.put(TransactionStatus.INITIATED, EnumSet.of(TransactionStatus.PENDING, TransactionStatus.FAILED));
        TRANSITIONS.put(TransactionStatus.PENDING, EnumSet.of(TransactionStatus.PROCESSING, TransactionStatus.FAILED));
        TRANSITIONS.put(TransactionStatus.PROCESSING, EnumSet.of(TransactionStatus.SUCCESS, TransactionStatus.FAILED));
        TRANSITIONS.put(TransactionStatus.SUCCESS, EnumSet.of(TransactionStatus.REVERSED));
        TRANSITIONS.put(TransactionStatus.FAILED, EnumSet.noneOf(TransactionStatus.class));
        TRANSITIONS.put(TransactionStatus.REVERSED, EnumSet.noneOf(TransactionStatus.class));
    }

    public void assertTransitionAllowed(TransactionStatus from, TransactionStatus to) {
        if (from == to) {
            return;
        }
        Set<TransactionStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalTransitionException("Illegal transaction status transition: " + from + " -> " + to);
        }
    }
}
