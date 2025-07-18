package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalIntegrationService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalWorkflowService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalFlowService;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalIntegrationServiceImpl implements ApprovalIntegrationService {

    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApprovalFlowService approvalFlowService;
    private final InvoiceDocRepo invoiceDocRepo;
    private final VoucherRepo voucherRepo;

    @Override
    public void submitForApproval(ServiceType serviceType, UUID itemId, UUID organisationId, UUID projectId)
            throws ItemNotFoundException, AccessDeniedException {

        ApprovalFlow flow = approvalFlowService.getApprovalFlowByService(organisationId, serviceType);
        updateDocumentStatus(serviceType, itemId, false);
        approvalWorkflowService.startApprovalWorkflow(serviceType, itemId, organisationId, projectId);
    }

    @Override
    public void handleApprovalComplete(ServiceType serviceType, UUID itemId, boolean approved) {
        updateDocumentStatus(serviceType, itemId, approved);

        if (approved) {
            executePostApprovalActions(serviceType, itemId);
        }
    }

    private void updateDocumentStatus(ServiceType serviceType, UUID itemId, boolean approved) {
        switch (serviceType) {
            case INVOICE -> {
                InvoiceDocEntity invoice = invoiceDocRepo.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Invoice not found"));

                if (approved) {
                    invoice.setInvoiceStatus(InvoiceStatus.APPROVED);
                } else {
                    invoice.setInvoiceStatus(InvoiceStatus.PENDING_APPROVAL);
                }
                invoiceDocRepo.save(invoice);
            }
            case VOUCHER -> {
                VoucherEntity voucher = voucherRepo.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Voucher not found"));

                if (approved) {
                    voucher.setStatus(VoucherStatus.APPROVED);
                } else {
                    voucher.setStatus(VoucherStatus.PENDING_APPROVAL);
                }
                voucherRepo.save(voucher);
            }
        }
    }

    private void executePostApprovalActions(ServiceType serviceType, UUID itemId) {
        switch (serviceType) {
            case INVOICE -> {
                // Invoice post-approval actions
            }
            case VOUCHER -> {
                // Voucher post-approval actions
            }
        }
    }
}