package com.bank.loan.api.v1.mapper;

import com.bank.loan.api.v1.dto.response.LoanResponse;
import com.bank.loan.domain.entity.Loan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanMapper {

    LoanResponse toResponse(Loan loan);
}
