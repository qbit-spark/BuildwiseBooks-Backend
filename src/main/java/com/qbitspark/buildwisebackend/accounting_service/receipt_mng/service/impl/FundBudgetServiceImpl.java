package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.BudgetFundingService;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl.BudgetFundingServiceImpl;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.FundBudgetService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FundBudgetServiceImpl implements FundBudgetService {
    private final ReceiptAllocationRepo receiptAllocationRepo;
    private final BudgetFundingService budgetFundingService;

    @Transactional
    @Override
    public void fundBudget( UUID allocationId) throws ItemNotFoundException, AccessDeniedException {

        ReceiptAllocationEntity receiptAllocationEntity = receiptAllocationRepo.findById(allocationId).orElseThrow(
                () -> new ItemNotFoundException("Receipt allocation not found")
        );

        if (receiptAllocationEntity.getStatus() != AllocationStatus.APPROVED) {
            throw new ItemNotFoundException("Receipt allocation is not approved");
        }

        List<BudgetFundingAllocationEntity> fundingAllocation = budgetFundingService.fundAccountsFromAllocation( receiptAllocationEntity);

        System.out.println("Funded allocations: " + fundingAllocation.size());

    }

}
