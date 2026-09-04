package com.bank.termdeposit.domain.service;

import com.bank.termdeposit.domain.entity.TermDeposit;
import com.bank.termdeposit.domain.repository.TermDepositRepository;
import com.bank.termdeposit.domain.statemachine.TermDepositStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background job that can race with a live premature-closure request on the
 * same row — {@code @Version} on {@link TermDeposit} makes that race
 * detectable, {@code TermDepositStateMachine} makes an illegal outcome of
 * it impossible to persist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MaturityProcessingScheduler {

    private final TermDepositRepository termDepositRepository;
    private final TermDepositService termDepositService;

    @Scheduled(cron = "${term-deposit.maturity-job.cron:0 0 1 * * *}")
    public void processMaturedDeposits() {
        List<TermDeposit> dueForMaturity =
                termDepositRepository.findByStatusAndMaturityDateLessThanEqual(TermDepositStatus.ACTIVE, LocalDate.now());
        for (TermDeposit deposit : dueForMaturity) {
            try {
                termDepositService.processMaturity(deposit.getId());
            } catch (Exception e) {
                log.error("Failed to process maturity for term deposit {}", deposit.getId(), e);
            }
        }
    }
}
