package com.bank.ft.domain.service;

import com.bank.ft.api.v1.dto.request.CreateScheduledTransferRequest;
import com.bank.ft.domain.entity.ScheduledTransfer;
import java.util.List;
import java.util.UUID;

public interface ScheduledTransferService {

    ScheduledTransfer createScheduledTransfer(
            CreateScheduledTransferRequest request, String cif, UUID keycloakUserId, String bankCode);

    ScheduledTransfer getScheduledTransfer(UUID scheduledTransferId);

    List<ScheduledTransfer> getScheduledTransfersForCif(String cif);

    ScheduledTransfer cancelScheduledTransfer(UUID scheduledTransferId);
}
