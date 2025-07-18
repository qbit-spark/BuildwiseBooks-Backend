package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailDistributionEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface OrgBudgetService {

    OrgBudgetEntity createBudget(CreateBudgetRequest request, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    List<OrgBudgetDetailDistributionEntity> distributeToDetails(UUID budgetId, DistributeToDetailsRequest request, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    List<OrgBudgetEntity> getBudgets(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    OrgBudgetEntity updateBudget(UUID budgetId, UpdateBudgetRequest request, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    BudgetDistributionDetailResponse getBudgetDistributionDetails(UUID budgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    void activateBudget(UUID budgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    BudgetAllocationResponse getBudgetAllocationSummary(UUID budgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;
}