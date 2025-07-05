package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgBudgetDetailAllocationRepo extends JpaRepository<OrgBudgetDetailAllocationEntity, UUID> {

    /**
     * Find all allocations for a specific header line item
     */
    List<OrgBudgetDetailAllocationEntity> findByHeaderLineItem(OrgBudgetLineItemEntity headerLineItem);

    /**
     * Find allocation by header line item and detail account
     */
    Optional<OrgBudgetDetailAllocationEntity> findByHeaderLineItemAndDetailAccount(
            OrgBudgetLineItemEntity headerLineItem,
            ChartOfAccounts detailAccount
    );

    /**
     * Find all allocations for a specific detail account
     */
    List<OrgBudgetDetailAllocationEntity> findByDetailAccount(ChartOfAccounts detailAccount);

    /**
     * Find allocations with money allocated (allocated amount > 0)
     */
    List<OrgBudgetDetailAllocationEntity> findByHeaderLineItemAndAllocatedAmountGreaterThan(
            OrgBudgetLineItemEntity headerLineItem,
            BigDecimal allocatedAmount
    );

    /**
     * Find allocations without money (allocated amount = 0)
     */
    List<OrgBudgetDetailAllocationEntity> findByHeaderLineItemAndAllocatedAmount(
            OrgBudgetLineItemEntity headerLineItem,
            BigDecimal allocatedAmount
    );

    /**
     * Check if allocation exists for header and detail account combination
     */
    boolean existsByHeaderLineItemAndDetailAccount(
            OrgBudgetLineItemEntity headerLineItem,
            ChartOfAccounts detailAccount
    );

    /**
     * Find allocations by detail account type
     */
    List<OrgBudgetDetailAllocationEntity> findByDetailAccountAccountType(AccountType accountType);

    /**
     * Find allocations by header line item ordered by detail account code
     */
    List<OrgBudgetDetailAllocationEntity> findByHeaderLineItemOrderByDetailAccountAccountCode(
            OrgBudgetLineItemEntity headerLineItem
    );

    /**
     * Find allocations with remaining amount (for available allocations)
     */
    List<OrgBudgetDetailAllocationEntity> findByHeaderLineItemAndAllocatedAmountGreaterThanAndSpentAmountLessThan(
            OrgBudgetLineItemEntity headerLineItem,
            BigDecimal minAllocated,
            BigDecimal maxSpent
    );

    /**
     * Count allocations for a header line item
     */
    long countByHeaderLineItem(OrgBudgetLineItemEntity headerLineItem);

    /**
     * Count allocations with money for a header line item
     */
    long countByHeaderLineItemAndAllocatedAmountGreaterThan(
            OrgBudgetLineItemEntity headerLineItem,
            BigDecimal allocatedAmount
    );
}