package com.bank.account.domain.repository;

import com.bank.account.domain.entity.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCif(String cif);

    boolean existsByAccountNumber(String accountNumber);
}
