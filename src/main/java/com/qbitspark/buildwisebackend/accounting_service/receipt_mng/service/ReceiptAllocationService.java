package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service;


import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateReceiptAllocationRequest;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.UUID;

public interface ReceiptAllocationService {
    ReceiptAllocationEntity createReceiptAllocation(UUID organisationId, CreateReceiptAllocationRequest request) throws ItemNotFoundException, AccessDeniedException;
}

