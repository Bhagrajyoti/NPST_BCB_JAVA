package com.bank.ft.api.v1.mapper;

import com.bank.ft.api.v1.dto.response.TransactionResponse;
import com.bank.ft.domain.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionResponse toResponse(Transaction transaction);
}
