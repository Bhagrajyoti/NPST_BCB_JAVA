package com.bank.account.api.v1.mapper;

import com.bank.account.api.v1.dto.response.AccountResponse;
import com.bank.account.domain.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);
}
