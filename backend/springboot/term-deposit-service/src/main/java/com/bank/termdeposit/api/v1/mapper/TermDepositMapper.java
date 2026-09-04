package com.bank.termdeposit.api.v1.mapper;

import com.bank.termdeposit.api.v1.dto.response.TermDepositResponse;
import com.bank.termdeposit.domain.entity.TermDeposit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TermDepositMapper {

    TermDepositResponse toResponse(TermDeposit termDeposit);
}
