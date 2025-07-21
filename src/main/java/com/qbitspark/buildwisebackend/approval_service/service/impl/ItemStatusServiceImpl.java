package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.BudgetFundingService;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.FundBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.ReceiptAllocationService;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.service.ItemStatusService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorStatus;
import com.qbitspark.buildwisebackend.vendormng_service.repo.VendorsRepo;
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
    private final VendorsRepo vendorsRepo;
    private final OrgBudgetRepo budgetRepo;
    private final ReceiptAllocationRepo receiptAllocationRepo;
    private final ReceiptRepo receiptRepo;
    private final FundBudgetService fundBudgetService;

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

            case VENDORS -> {
                VendorEntity vendor = vendorsRepo.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Vendor not found"));

                if (approved) {
                    vendor.setStatus(VendorStatus.APPROVED);
                } else {
                    vendor.setStatus(VendorStatus.PENDING_APPROVAL);
                }
                vendorsRepo.save(vendor);
            }

            case BUDGET -> {
                OrgBudgetEntity budget = budgetRepo.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Budget not found"));

                if (approved) {
                    budget.setStatus(OrgBudgetStatus.APPROVED);
                } else {
                    budget.setStatus(OrgBudgetStatus.PENDING_APPROVAL);
                }
                budgetRepo.save(budget);
            }

            case RECEIPT -> {
                ReceiptEntity receipt = receiptRepo.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Receipt not found"));

                if (approved) {
                    receipt.setStatus(ReceiptStatus.APPROVED);
                } else {
                    receipt.setStatus(ReceiptStatus.PENDING_APPROVAL);
                }
                receiptRepo.save(receipt);
            }

            case RECEIPT_ALLOCATIONS_TO_BUDGET -> {
                ReceiptAllocationEntity allocation = receiptAllocationRepo.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Receipt allocation not found"));

                if (approved) {
                    allocation.setStatus(AllocationStatus.APPROVED);
                } else {
                    allocation.setStatus(AllocationStatus.PENDING_APPROVAL);
                }
                receiptAllocationRepo.save(allocation);
            }
        }
    }

    @Override
    public void executePostApprovalActions(ServiceType serviceType, UUID itemId) throws AccessDeniedException, ItemNotFoundException {
        switch (serviceType) {
            case INVOICE -> {
                // Invoice post-approval actions
                // Could trigger accounting entries, notifications, etc.
            }
            case VOUCHER -> {
                // Voucher post-approval actions
                // Could update budget allocations, etc.
            }
            case RECEIPT_ALLOCATIONS_TO_BUDGET -> {
               fundBudgetService.fundBudget(itemId);
            }
        }
    }
}