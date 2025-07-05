package com.qbitspark.buildwisebackend.accounting_service.bank_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.entity.BankAccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepo extends JpaRepository<BankAccountEntity, UUID> {

    List<BankAccountEntity> findByOrganisationAndIsActiveTrue(OrganisationEntity organisation);

    Optional<BankAccountEntity> findByOrganisationAndIsDefaultTrue(OrganisationEntity organisation);

    Optional<BankAccountEntity> findByAccountNumber(String accountNumber);

    Optional<BankAccountEntity> findByBankAccountIdAndOrganisation(UUID bankAccountId, OrganisationEntity organisation);

    boolean existsByAccountNumberAndOrganisation(String accountNumber, OrganisationEntity organisation);
}