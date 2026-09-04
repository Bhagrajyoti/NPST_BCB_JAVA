package com.bank.account.domain.service;

import com.bank.account.api.v1.dto.request.CreateAccountRequest;
import com.bank.account.domain.entity.Account;
import java.util.List;
import java.util.UUID;

public interface AccountService {

    Account createAccount(CreateAccountRequest request, UUID keycloakUserId, String bankCode);

    Account getAccount(UUID accountId);

    List<Account> getAccountsForCif(String cif);

    Account freezeAccount(UUID accountId);

    Account reactivateAccount(UUID accountId);

    Account closeAccount(UUID accountId);
}
