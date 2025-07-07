package com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.entity.ProjectBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.entity.ProjectBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Deprecated
public interface ProjectBudgetLineItemRepo extends JpaRepository<ProjectBudgetLineItemEntity, UUID> {

    List<ProjectBudgetLineItemEntity> findByProjectBudget(ProjectBudgetEntity projectBudget);

    Optional<ProjectBudgetLineItemEntity> findByProjectBudgetAndChartOfAccount(
            ProjectBudgetEntity projectBudget,
            ChartOfAccounts chartOfAccount
    );

    List<ProjectBudgetLineItemEntity> findByChartOfAccount(ChartOfAccounts chartOfAccount);
}
