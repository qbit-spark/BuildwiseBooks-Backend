package com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.entity.ProjectBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.payload.DistributeBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.payload.ProjectBudgetSummaryResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;

import java.util.List;
import java.util.UUID;

@Deprecated
public interface ProjectBudgetService {

    //This is useful if we have a fixed budget in accounts
    void initialiseProjectBudget(OrgBudgetEntity orgBudget, ProjectEntity project)
            throws ItemNotFoundException;

    ProjectBudgetEntity distributeBudgetToProject(DistributeBudgetRequest request, UUID projectBudgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    ProjectBudgetEntity getProjectBudget(UUID projectId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    List<ProjectBudgetSummaryResponse> getProjectBudgetSummary(UUID projectId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

}