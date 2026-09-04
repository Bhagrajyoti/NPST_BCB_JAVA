package com.bank.account.domain.statemachine;

import com.bank.account.common.exception.IllegalTransitionException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Every legal transition is listed explicitly. Anything not in this map is
 * illegal by default — throws rather than silently allowing a status
 * mutation nobody reviewed.
 */
@Component
public class AccountStateMachine {

    private static final Map<AccountStatus, Set<AccountStatus>> TRANSITIONS = new EnumMap<>(AccountStatus.class);

    static {
        TRANSITIONS.put(AccountStatus.PENDING_ACTIVATION, EnumSet.of(AccountStatus.ACTIVE, AccountStatus.CLOSED));
        TRANSITIONS.put(AccountStatus.ACTIVE, EnumSet.of(AccountStatus.DORMANT, AccountStatus.FROZEN, AccountStatus.CLOSED));
        TRANSITIONS.put(AccountStatus.DORMANT, EnumSet.of(AccountStatus.ACTIVE, AccountStatus.FROZEN, AccountStatus.CLOSED));
        TRANSITIONS.put(AccountStatus.FROZEN, EnumSet.of(AccountStatus.ACTIVE, AccountStatus.CLOSED));
        TRANSITIONS.put(AccountStatus.CLOSED, EnumSet.noneOf(AccountStatus.class));
    }

    public void assertTransitionAllowed(AccountStatus from, AccountStatus to) {
        if (from == to) {
            return;
        }
        Set<AccountStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalTransitionException(
                    "Illegal account status transition: " + from + " -> " + to);
        }
    }
}
