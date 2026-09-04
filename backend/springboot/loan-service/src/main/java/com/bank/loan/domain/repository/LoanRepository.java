package com.bank.loan.domain.repository;

import com.bank.loan.domain.entity.Loan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByCif(String cif);
}
