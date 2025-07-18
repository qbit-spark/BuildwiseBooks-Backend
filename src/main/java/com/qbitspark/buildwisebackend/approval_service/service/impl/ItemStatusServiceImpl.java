package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.service.ItemStatusService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemStatusServiceImpl implements ItemStatusService {

    private final InvoiceDocRepo invoiceDocRepo;
    private final VoucherRepo voucherRepo;

    @Override
    public void updateItemStatus(ServiceType serviceType, UUID itemId, boolean approved) {
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

    @Override
    public void executePostApprovalActions(ServiceType serviceType, UUID itemId) {
        switch (serviceType) {
            case INVOICE -> {
                // Invoice post-approval actions
                // Could trigger accounting entries, notifications, etc.
            }
            case VOUCHER -> {
                // Voucher post-approval actions
                // Could update budget allocations, etc.
            }
        }
    }
}