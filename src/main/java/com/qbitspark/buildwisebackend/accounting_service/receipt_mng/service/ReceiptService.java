package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AvailableDetailAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.CreateReceiptRequest;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.UpdateReceiptRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReceiptService {

    ReceiptEntity createReceipt(UUID organisationId, CreateReceiptRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    Page<ReceiptEntity> getOrganisationReceipts(UUID organisationId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException;

    ReceiptEntity getReceiptById(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException;

    ReceiptEntity updateReceipt(UUID organisationId, UUID receiptId, UpdateReceiptRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    void confirmReceipt(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException;

    void cancelReceipt(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException;

    List<ReceiptEntity> getInvoicePayments(UUID organisationId, UUID invoiceId)
            throws ItemNotFoundException, AccessDeniedException;

    Page<ReceiptEntity> getProjectReceipts(UUID organisationId, UUID projectId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException;

}