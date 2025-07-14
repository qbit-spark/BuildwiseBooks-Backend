package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service;


import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.CreateReceiptAllocationRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ReceiptAllocationService {

    ReceiptAllocationEntity createAllocation(UUID organisationId, CreateReceiptAllocationRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    ReceiptAllocationEntity updateAllocation(UUID organisationId, UUID allocationId,
                                             CreateReceiptAllocationRequest request) throws ItemNotFoundException, AccessDeniedException;

    void deleteAllocation(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException;

    ReceiptAllocationEntity getAllocationById(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException;

    List<ReceiptAllocationEntity> getReceiptAllocations(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException;

    ReceiptAllocationEntity activateAllocation(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException;

    ReceiptAllocationEntity deactivateAllocation(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException;
}