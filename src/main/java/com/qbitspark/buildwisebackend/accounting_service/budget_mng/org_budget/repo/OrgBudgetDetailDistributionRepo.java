package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailDistributionEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrgBudgetDetailDistributionRepo extends JpaRepository<OrgBudgetDetailDistributionEntity, UUID> {

    List<OrgBudgetDetailDistributionEntity> findByBudget(OrgBudgetEntity budget);

    List<OrgBudgetDetailDistributionEntity> findByDetailAccount(ChartOfAccounts detailAccount);

    List<OrgBudgetDetailDistributionEntity> findByBudgetAndDetailAccount(
            OrgBudgetEntity budget, ChartOfAccounts detailAccount);
}