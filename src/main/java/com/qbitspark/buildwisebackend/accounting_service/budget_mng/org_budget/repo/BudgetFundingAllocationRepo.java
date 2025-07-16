package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface BudgetFundingAllocationRepo extends JpaRepository<BudgetFundingAllocationEntity, UUID> {
    List<BudgetFundingAllocationEntity> findByBudgetAndAccount(OrgBudgetEntity budget, ChartOfAccounts account);

    List<BudgetFundingAllocationEntity> findByAccount(ChartOfAccounts account);

    List<BudgetFundingAllocationEntity> findByBudget(OrgBudgetEntity budget);

}