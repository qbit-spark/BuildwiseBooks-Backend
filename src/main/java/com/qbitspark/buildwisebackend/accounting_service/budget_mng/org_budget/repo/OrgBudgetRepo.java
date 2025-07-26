package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface OrgBudgetRepo extends JpaRepository<OrgBudgetEntity, UUID> {

    List<OrgBudgetEntity> findByOrganisation(OrganisationEntity organisation);

    Optional<OrgBudgetEntity> findByOrganisationAndStatus(OrganisationEntity organisation, OrgBudgetStatus status);

    List<OrgBudgetEntity> findByOrganisationAndFinancialYearStartLessThanEqualAndFinancialYearEndGreaterThanEqual(
            OrganisationEntity organisation,
            LocalDate endDate,
            LocalDate startDate
    );

}
