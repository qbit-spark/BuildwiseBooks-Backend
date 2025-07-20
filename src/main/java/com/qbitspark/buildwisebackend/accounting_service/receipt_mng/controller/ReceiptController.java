package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.controller;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.impl.InvoiceDocServiceImpl;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.*;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.ReceiptService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;
    private final InvoiceDocServiceImpl invoiceDocService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createReceipt(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateReceiptRequest request,  @RequestParam(value = "action") ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        ReceiptEntity receipt = receiptService.createReceipt(organisationId, request, action);
        ReceiptResponse response = mapToResponse(receipt);

        String successMessage = switch (action) {
            case SAVE -> "Receipt saved successfully";
            case SAVE_AND_APPROVAL -> "Receipt saved and submitted for approval";
        };

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(successMessage, response)
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationReceipts(
            @PathVariable UUID organisationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receiptDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection)
            throws ItemNotFoundException, AccessDeniedException {

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReceiptEntity> receiptsPage = receiptService.getOrganisationReceipts(organisationId, pageable);

        Page<ReceiptResponse> responsePage = receiptsPage.map(this::mapToResponse);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Receipts retrieved successfully", responsePage)
        );
    }

    @GetMapping("/summary-list")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationReceiptsSummary(
            @PathVariable UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        List<ReceiptEntity> receipts = receiptService.getOrganisationReceiptsSummary(organisationId);

        List<ReceiptSummaryResponse> summaryList = receipts.stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Receipt summary retrieved successfully", summaryList)
        );
    }


    @GetMapping("/{receiptId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getReceiptById(
            @PathVariable UUID organisationId,
            @PathVariable UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        ReceiptEntity receipt = receiptService.getReceiptById(organisationId, receiptId);
        ReceiptResponse response = mapToResponse(receipt);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Receipt retrieved successfully", response)
        );
    }

    @PutMapping("/{receiptId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateReceipt(
            @PathVariable UUID organisationId,
            @PathVariable UUID receiptId,
            @Valid @RequestBody UpdateReceiptRequest request, @RequestParam(value = "action") ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        ReceiptEntity receipt = receiptService.updateReceipt(organisationId, receiptId, request, action);
        ReceiptResponse response = mapToResponse(receipt);

        String successMessage = switch (action) {
            case SAVE -> "Receipt saved successfully";
            case SAVE_AND_APPROVAL -> "Receipt saved and submitted for approval";
        };

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(successMessage, response)
        );
    }

    @PutMapping("/{receiptId}/confirm")
    public ResponseEntity<GlobeSuccessResponseBuilder> confirmReceipt(
            @PathVariable UUID organisationId,
            @PathVariable UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        receiptService.confirmReceipt(organisationId, receiptId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Receipt confirmed successfully")
        );
    }

    @PutMapping("/{receiptId}/cancel")
    public ResponseEntity<GlobeSuccessResponseBuilder> cancelReceipt(
            @PathVariable UUID organisationId,
            @PathVariable UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        receiptService.cancelReceipt(organisationId, receiptId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Receipt cancelled successfully")
        );
    }

    @GetMapping("/invoice/{invoiceId}/payments")
    public ResponseEntity<GlobeSuccessResponseBuilder> getInvoicePayments(
            @PathVariable UUID organisationId,
            @PathVariable UUID invoiceId)
            throws ItemNotFoundException, AccessDeniedException {

        List<ReceiptEntity> receipts = receiptService.getInvoicePayments(organisationId, invoiceId);
        List<PaymentHistoryResponse> paymentHistory = mapToPaymentHistory(receipts);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Invoice payment history retrieved successfully", paymentHistory)
        );
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectReceipts(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size)
            throws ItemNotFoundException, AccessDeniedException {

        Pageable pageable = PageRequest.of(page, size, Sort.by("receiptDate").descending());
        Page<ReceiptEntity> receiptsPage = receiptService.getProjectReceipts(organisationId, projectId, pageable);

        Page<ReceiptResponse> responsePage = receiptsPage.map(this::mapToResponse);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Project receipts retrieved successfully", responsePage)
        );
    }


    private ReceiptResponse mapToResponse(ReceiptEntity entity) {
        ReceiptResponse response = new ReceiptResponse();
        response.setReceiptId(entity.getReceiptId());
        response.setReceiptNumber(entity.getReceiptNumber());
        response.setReceiptDate(entity.getReceiptDate());
        response.setOrganisationId(entity.getOrganisation().getOrganisationId());
        response.setProjectId(entity.getProject() != null ? entity.getProject().getProjectId() : null);
        response.setClientId(entity.getClient().getClientId());
        response.setInvoiceId(entity.getInvoice().getId());
        response.setBankAccountId(entity.getBankAccount() != null ? entity.getBankAccount().getBankAccountId() : null);
        response.setTotalAmount(entity.getTotalAmount());
        response.setPaymentMethod(entity.getPaymentMethod());
        response.setStatus(entity.getStatus());
        response.setReference(entity.getReference());
        response.setDescription(entity.getDescription());
        response.setAttachments(entity.getAttachments());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        response.setClientName(entity.getClient().getName());
        response.setInvoiceNumber(entity.getInvoice().getInvoiceNumber());
        response.setProjectName(entity.getProject() != null ? entity.getProject().getName() : null);
        response.setBankAccountName(entity.getBankAccount() != null ? entity.getBankAccount().getAccountName() : null);
        response.setInvoiceTotal(entity.getInvoice().getTotalAmount());

        response.setInvoicePreviousPaid(invoiceDocService.calculateAmountPaid(entity.getInvoice().getId()));
        response.setInvoiceNewBalance(invoiceDocService.calculateAmountDue(entity.getInvoice()));
        response.setInvoiceStatus(entity.getInvoice().getInvoiceStatus().name());

        return response;
    }

    private List<PaymentHistoryResponse> mapToPaymentHistory(List<ReceiptEntity> receipts) {
        AtomicReference<BigDecimal> cumulativeTotal = new AtomicReference<>(BigDecimal.ZERO);

        return receipts.stream()
                .map(receipt -> {
                    cumulativeTotal.updateAndGet(current -> current.add(receipt.getTotalAmount()));

                    PaymentHistoryResponse history = new PaymentHistoryResponse();
                    history.setReceiptId(receipt.getReceiptId());
                    history.setReceiptNumber(receipt.getReceiptNumber());
                    history.setReceiptDate(receipt.getReceiptDate());
                    history.setAmount(receipt.getTotalAmount());
                    history.setPaymentMethod(receipt.getPaymentMethod());
                    history.setReference(receipt.getReference());
                    history.setCumulativeTotal(cumulativeTotal.get());
                    history.setStatus(receipt.getStatus());

                    return history;
                })
                .collect(Collectors.toList());
    }

    private ReceiptSummaryResponse mapToSummaryResponse(ReceiptEntity receipt) {
        ReceiptSummaryResponse response = new ReceiptSummaryResponse();
        response.setReceiptId(receipt.getReceiptId());
        response.setProjectName(receipt.getProject() != null ? receipt.getProject().getName() : null);
        response.setReceiptNumber(receipt.getReceiptNumber());
        response.setReceiptDate(receipt.getReceiptDate());
        response.setClientName(receipt.getClient() != null ? receipt.getClient().getName() : null);
        response.setTotalAmount(receipt.getTotalAmount());
        response.setPaymentMethod(receipt.getPaymentMethod());
        response.setStatus(receipt.getStatus());
        response.setReference(receipt.getReference());
        return response;
    }
}