package com.bank.ft.api.v1.mapper;

import com.bank.ft.api.v1.dto.response.ScheduledTransferResponse;
import com.bank.ft.domain.entity.ScheduledTransfer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScheduledTransferMapper {

    ScheduledTransferResponse toResponse(ScheduledTransfer scheduledTransfer);
}
