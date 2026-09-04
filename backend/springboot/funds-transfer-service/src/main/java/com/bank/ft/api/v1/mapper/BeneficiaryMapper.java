package com.bank.ft.api.v1.mapper;

import com.bank.ft.api.v1.dto.response.BeneficiaryResponse;
import com.bank.ft.domain.entity.Beneficiary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BeneficiaryMapper {

    BeneficiaryResponse toResponse(Beneficiary beneficiary);
}
