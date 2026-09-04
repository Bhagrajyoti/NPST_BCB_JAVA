package com.bank.ft.domain.repository;

import com.bank.ft.domain.entity.ScheduledTransfer;
import com.bank.ft.domain.statemachine.ScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, UUID> {

    List<ScheduledTransfer> findByCif(String cif);

    List<ScheduledTransfer> findByStatusAndNextExecutionDateLessThanEqual(ScheduleStatus status, LocalDate date);
}
