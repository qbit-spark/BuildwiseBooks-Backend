package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BudgetFundingService {

    BigDecimal getAvailableBudget(UUID organisationId, UUID budgetId, UUID accountId) throws ItemNotFoundException, AccessDeniedException;

    List<BudgetFundingAllocationEntity> fundAccountsFromAllocation(UUID organisationId, ReceiptAllocationEntity allocation) throws ItemNotFoundException, AccessDeniedException;

    List<BudgetFundingAllocationEntity> getAccountFundingHistory(UUID organisationId, UUID budgetId, UUID accountId) throws ItemNotFoundException, AccessDeniedException;

}