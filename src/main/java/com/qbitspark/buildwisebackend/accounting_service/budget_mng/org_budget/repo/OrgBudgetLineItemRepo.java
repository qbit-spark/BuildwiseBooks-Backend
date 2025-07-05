package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgBudgetLineItemRepo extends JpaRepository<OrgBudgetLineItemEntity, UUID> {

    /**
     * Find all line items for a specific organization budget
     */
    List<OrgBudgetLineItemEntity> findByOrgBudget(OrgBudgetEntity orgBudget);

    /**
     * Find a specific line item by org budget and chart of account
     */
    Optional<OrgBudgetLineItemEntity> findByOrgBudgetAndChartOfAccount(
            OrgBudgetEntity orgBudget,
            ChartOfAccounts chartOfAccount
    );

    /**
     * Find all line items for a specific chart of account across all org budgets
     */
    List<OrgBudgetLineItemEntity> findByChartOfAccount(ChartOfAccounts chartOfAccount);

    /**
     * Find line items with budget allocated (budget amount > 0)
     */
    List<OrgBudgetLineItemEntity> findByOrgBudgetAndBudgetAmountGreaterThan(
            OrgBudgetEntity orgBudget,
            BigDecimal budgetAmount
    );

    /**
     * Find line items without budget allocated (budget amount = 0)
     */
    List<OrgBudgetLineItemEntity> findByOrgBudgetAndBudgetAmount(
            OrgBudgetEntity orgBudget,
            BigDecimal budgetAmount
    );

    /**
     * Find line items by chart of account type for an organization budget
     */
    List<OrgBudgetLineItemEntity> findByOrgBudgetAndChartOfAccountAccountType(
            OrgBudgetEntity orgBudget,
            AccountType accountType
    );

    /**
     * Check if a chart of account has budget line item in the given org budget
     */
    boolean existsByOrgBudgetAndChartOfAccount(OrgBudgetEntity orgBudget, ChartOfAccounts chartOfAccount);

    /**
     * Count total line items for an organization budget
     */
    long countByOrgBudget(OrgBudgetEntity orgBudget);

    /**
     * Find line items by org budget ordered by chart of account code
     */
    List<OrgBudgetLineItemEntity> findByOrgBudgetOrderByChartOfAccountAccountCode(OrgBudgetEntity orgBudget);

    /**
     * Find line items by org budget and chart of account is active
     */
    List<OrgBudgetLineItemEntity> findByOrgBudgetAndChartOfAccountIsActive(
            OrgBudgetEntity orgBudget,
            Boolean isActive
    );

    /**
     * Find line items by org budget and chart of account is postable
     */
    List<OrgBudgetLineItemEntity> findByOrgBudgetAndChartOfAccountIsPostable(
            OrgBudgetEntity orgBudget,
            Boolean isPostable
    );
}