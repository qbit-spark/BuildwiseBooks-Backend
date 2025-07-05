package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AllocateMoneyRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AllocationSummaryResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface OrgBudgetAllocationService {

    /**
     * Allocate money to detail accounts within a header account
     */
    List<OrgBudgetDetailAllocationEntity> allocateMoneyToDetailAccounts(
            UUID organisationId,
            UUID budgetId,
            UUID headerLineItemId,
            AllocateMoneyRequest request
    ) throws ItemNotFoundException;

    /**
     * Initialize detail account allocations for a header (with Tsh 0)
     */
    void initializeDetailAllocations(UUID headerLineItemId, UUID organisationId) throws ItemNotFoundException;

    /**
     * Get all allocations for a header account
     */
    List<OrgBudgetDetailAllocationEntity> getHeaderAllocations(
            UUID organisationId,
            UUID budgetId,
            UUID headerLineItemId
    ) throws ItemNotFoundException;

    /**
     * Get allocation summary for a header account
     */
    AllocationSummaryResponse getAllocationSummary(
            UUID organisationId,
            UUID budgetId,
            UUID headerLineItemId
    ) throws ItemNotFoundException;

    /**
     * Get all allocations for an entire budget (all headers)
     */
    List<OrgBudgetDetailAllocationEntity> getAllBudgetAllocations(
            UUID organisationId,
            UUID budgetId
    ) throws ItemNotFoundException;
}