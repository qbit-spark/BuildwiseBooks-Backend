package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service;


import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateReceiptAllocationRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.ReceiptAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.ReceiptAllocationSummaryResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReceiptAllocationService {
    ReceiptAllocationEntity createReceiptAllocation(UUID organisationId, CreateReceiptAllocationRequest request, ActionType actionType) throws ItemNotFoundException, AccessDeniedException;

    Page<ReceiptAllocationSummaryResponse> getAllocations(UUID organisationId, AllocationStatus status,
                                                          int page, int size) throws ItemNotFoundException, AccessDeniedException;

    ReceiptAllocationResponse getAllocationDetails(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException;


}

