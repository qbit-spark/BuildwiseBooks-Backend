package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AllocateMoneyRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AllocationSummaryResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AvailableDetailAllocationResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface OrgBudgetAllocationService {


    List<OrgBudgetDetailAllocationEntity> allocateMoneyToDetailAccounts(
            UUID organisationId,
            UUID budgetId,
            AllocateMoneyRequest request
    ) throws ItemNotFoundException, AccessDeniedException;


    List<OrgBudgetDetailAllocationEntity> getHeaderAllocations(
            UUID organisationId,
            UUID budgetId,
            UUID headerLineItemId
    ) throws ItemNotFoundException, AccessDeniedException;


    AllocationSummaryResponse getAllocationSummary(
            UUID organisationId,
            UUID budgetId,
            UUID headerLineItemId
    ) throws ItemNotFoundException, AccessDeniedException;


    List<OrgBudgetDetailAllocationEntity> getAllBudgetAllocations(
            UUID organisationId,
            UUID budgetId
    ) throws ItemNotFoundException, AccessDeniedException;


    List<AvailableDetailAllocationResponse> getDetailAccountsForVouchers(
            UUID organisationId, UUID budgetId) throws ItemNotFoundException, AccessDeniedException;

}