package com.qbitspark.buildwisebackend.accounting_service.coa.repo;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChartOfAccountsRepo extends JpaRepository<ChartOfAccounts, UUID> {
    List<ChartOfAccounts> findByOrganisation_OrganisationId(UUID organisationId);

    List<ChartOfAccounts> findByOrganisationAndAccountTypeAndIsActiveAndIsPostable(
            OrganisationEntity organisation,
            AccountType accountType,
            Boolean isActive,
            Boolean isPostable
    );

    // Add to ChartOfAccountsRepo
    List<ChartOfAccounts> findByOrganisationAndAccountTypeAndIsActive(
            OrganisationEntity organisation,
            AccountType accountType,
            Boolean isActive
    );
}
