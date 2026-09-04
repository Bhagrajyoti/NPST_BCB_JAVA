package com.bank.ft.domain.repository;

import com.bank.ft.domain.entity.Transaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionReference(String transactionReference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByInitiatorCifOrderByInitiatedAtDesc(String initiatorCif);
}
