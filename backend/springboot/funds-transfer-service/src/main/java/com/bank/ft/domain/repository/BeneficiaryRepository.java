package com.bank.ft.domain.repository;

import com.bank.ft.domain.entity.Beneficiary;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByOwnerCif(String ownerCif);
}
