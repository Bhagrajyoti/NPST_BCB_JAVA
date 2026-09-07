package com.bank.ft.domain.service;

import com.bank.ft.api.v1.dto.request.CreateScheduledTransferRequest;
import com.bank.ft.common.exception.IllegalTransitionException;
import com.bank.ft.common.exception.ResourceNotFoundException;
import com.bank.ft.domain.entity.ScheduledTransfer;
import com.bank.ft.domain.repository.ScheduledTransferRepository;
import com.bank.ft.domain.statemachine.ScheduleStatus;
import com.bank.ft.domain.statemachine.TransferMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduledTransferServiceImpl implements ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;

    @Override
    @Transactional
    public ScheduledTransfer createScheduledTransfer(
            CreateScheduledTransferRequest request, String cif, UUID keycloakUserId, String bankCode) {
        ScheduledTransfer scheduledTransfer = ScheduledTransfer.builder()
                .cif(cif)
                .keycloakUserId(keycloakUserId)
                .beneficiaryId(request.beneficiaryId())
                .amountMinorUnits(request.amountMinorUnits())
                .transferMode(request.transferMode())
                .frequency(request.frequency())
                .nextExecutionDate(request.nextExecutionDate())
                .endDate(request.endDate())
                .maxRetries(request.maxRetries() != null ? request.maxRetries() : 3)
                .status(ScheduleStatus.ACTIVE)
                .bankCode(bankCode)
                .remarks(request.remarks())
                .build();
        return scheduledTransferRepository.save(scheduledTransfer);
    }

    @Override
    public ScheduledTransfer getScheduledTransfer(UUID scheduledTransferId) {
        return scheduledTransferRepository.findById(scheduledTransferId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled transfer not found: " + scheduledTransferId));
    }

    @Override
    public List<ScheduledTransfer> getScheduledTransfersForCif(String cif) {
        return scheduledTransferRepository.findByCif(cif);
    }

    @Override
    @Transactional
    public ScheduledTransfer cancelScheduledTransfer(UUID scheduledTransferId) {
        ScheduledTransfer scheduledTransfer = getScheduledTransfer(scheduledTransferId);
        if (scheduledTransfer.getStatus() == ScheduleStatus.COMPLETED || scheduledTransfer.getStatus() == ScheduleStatus.CANCELLED) {
            throw new IllegalTransitionException("Cannot cancel a completed or already cancelled scheduled transfer: " + scheduledTransferId);
        }
        scheduledTransfer.setStatus(ScheduleStatus.CANCELLED);
        scheduledTransfer.setUpdatedAt(Instant.now());
        return scheduledTransferRepository.save(scheduledTransfer);
    }
}
