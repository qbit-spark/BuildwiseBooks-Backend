package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload.DistributeBudgetRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ProjectBudgetService {
    List<ProjectBudgetEntity> distributeBudgetToProject(DistributeBudgetRequest request, UUID orgBudgetId, UUID projectId) throws ItemNotFoundException, AccessDeniedException;

}