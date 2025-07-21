package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AvailableDetailAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BudgetFundingService {
    List<BudgetFundingAllocationEntity> fundAccountsFromAllocation(ReceiptAllocationEntity allocation) throws ItemNotFoundException, AccessDeniedException;

    List<AvailableDetailAllocationResponse> getAvailableDetailAllocations(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

}