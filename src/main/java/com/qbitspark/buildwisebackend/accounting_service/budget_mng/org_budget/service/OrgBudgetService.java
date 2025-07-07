package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface OrgBudgetService {

    OrgBudgetEntity createBudget(CreateBudgetRequest request, UUID organisationId) throws ItemNotFoundException;

    void activateBudget(UUID budgetId, UUID organisationId) throws ItemNotFoundException;
    List<OrgBudgetEntity> getBudgets(UUID organisationId) throws ItemNotFoundException;
    OrgBudgetEntity updateBudget(UUID budgetId, UpdateBudgetRequest request, UUID organisationId) throws ItemNotFoundException;


    OrgBudgetEntity distributeBudget(UUID budgetId, DistributeBudgetRequest request, UUID organisationId) throws ItemNotFoundException;


    void initializeBudgetWithAccounts(UUID budgetId, UUID organisationId) throws ItemNotFoundException;


    OrgBudgetSummaryResponse getBudgetSummary(UUID budgetId, UUID organisationId) throws ItemNotFoundException;


    OrgBudgetEntity getBudgetWithAccounts(UUID budgetId, UUID organisationId) throws ItemNotFoundException;

    BudgetHierarchyWithAllocationsResponse getBudgetHierarchyWithAllocations(
            UUID budgetId, UUID organisationId) throws ItemNotFoundException;

}