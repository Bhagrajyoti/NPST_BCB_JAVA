package com.bank.termdeposit.domain.repository;

import com.bank.termdeposit.domain.entity.TermDeposit;
import com.bank.termdeposit.domain.statemachine.TermDepositStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermDepositRepository extends JpaRepository<TermDeposit, UUID> {

    List<TermDeposit> findByCif(String cif);

    List<TermDeposit> findByStatusAndMaturityDateLessThanEqual(TermDepositStatus status, LocalDate date);
}
