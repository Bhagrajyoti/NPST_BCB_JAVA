package com.bank.ft.domain.service;

import com.bank.ft.api.v1.dto.request.InitiateTransferRequest;
import com.bank.ft.domain.entity.Transaction;
import java.util.List;
import java.util.UUID;

public interface TransferService {

    Transaction initiateTransfer(InitiateTransferRequest request, String initiatorCif, UUID initiatorKeycloakUserId,
                                  String bankCode, String idempotencyKey);

    Transaction getTransaction(UUID transactionId);

    List<Transaction> getTransactionsForCif(String initiatorCif);
}
