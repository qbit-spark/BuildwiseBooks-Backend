package com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.controller;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;

import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.dto.*;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.user_transaction.ExpenseEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.user_transaction.InvoiceEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.user_transaction.SimpleTransactionEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.paylaods.JournalEntryRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.transaction_service.TransactionService;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    // ====================================================================
    // SIMPLE TRANSACTIONS - Most common construction transactions_pipeline
    // ====================================================================

    @PostMapping("/simple")
    public ResponseEntity<GlobeSuccessResponseBuilder> createSimpleTransaction(
            @RequestBody SimpleTransactionRequest request) throws Exception {

        log.info("Creating simple transaction: {} for organisation: {}",
                request.getDescription(), request.getOrganisationId());

        // Create business event
        SimpleTransactionEvent event = new SimpleTransactionEvent(
                request.getDebitAccountId(),
                request.getCreditAccountId(),
                request.getAmount(),
                request.getDescription()
        );
        event.setOrganisationId(request.getOrganisationId());
        event.setProjectId(request.getProjectId());
        event.setReferenceNumber(request.getReferenceNumber());

        // Process through our complete transaction pipeline
        JournalEntry result = transactionService.processBusinessEvent(event);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Simple transaction created successfully",
                        mapToTransactionResponse(result)
                )
        );
    }

    // ====================================================================
    // INVOICE TRANSACTIONS - Customer billing with multiple line items
    // ====================================================================

    @PostMapping("/invoice")
    public ResponseEntity<GlobeSuccessResponseBuilder> createInvoiceTransaction(
            @RequestBody InvoiceTransactionRequest request) throws Exception {

        log.info("Creating invoice transaction for customer: {} amount: {}",
                request.getCustomerId(), request.getTotalAmount());

        // Create invoice business event
        InvoiceEvent event = new InvoiceEvent();
        event.setOrganisationId(request.getOrganisationId());
        event.setProjectId(request.getProjectId());
        event.setCustomerId(request.getCustomerId());
        event.setTotalAmount(request.getTotalAmount());
        event.setTaxAmount(request.getTaxAmount());
        event.setDescription(request.getDescription());
        event.setReferenceNumber(request.getReferenceNumber());
        event.setLineItems(request.getLineItems());

        // Process through our complete transaction pipeline
        JournalEntry result = transactionService.processBusinessEvent(event);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invoice transaction created successfully",
                        mapToTransactionResponse(result)
                )
        );
    }

    // ====================================================================
    // EXPENSE TRANSACTIONS - Business expenses with vendor tracking
    // ====================================================================

    @PostMapping("/expense")
    public ResponseEntity<GlobeSuccessResponseBuilder> createExpenseTransaction(
            @RequestBody ExpenseTransactionRequest request) throws Exception {

        log.info("Creating expense transaction: {} amount: {} paid: {}",
                request.getDescription(), request.getAmount(), request.isPaid());

        // Create expense business event
        ExpenseEvent event = new ExpenseEvent();
        event.setOrganisationId(request.getOrganisationId());
        event.setProjectId(request.getProjectId());
        event.setVendorId(request.getVendorId());
        event.setExpenseAccountId(request.getExpenseAccountId());
        event.setAmount(request.getAmount());
        event.setCategory(request.getCategory());
        event.setPaid(request.isPaid());
        event.setDescription(request.getDescription());
        event.setReferenceNumber(request.getReferenceNumber());

        // Process through our complete transaction pipeline
        JournalEntry result = transactionService.processBusinessEvent(event);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Expense transaction created successfully",
                        mapToTransactionResponse(result)
                )
        );
    }

    // ====================================================================
    // DIRECT JOURNAL ENTRY - For advanced users or system integrations
    // ====================================================================

    @PostMapping("/journal-entry")
    public ResponseEntity<GlobeSuccessResponseBuilder> createJournalEntry(
            @RequestBody JournalEntryRequest request) throws Exception {

        log.info("Creating direct journal entry: {} with {} lines",
                request.getDescription(), request.getLineCount());

        // Process directly through transaction service (bypassing business events)
        JournalEntry result = transactionService.createJournalEntry(request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Journal entry created successfully",
                        mapToTransactionResponse(result)
                )
        );
    }

    // ====================================================================
    // APPROVAL ENDPOINTS - For transactions_pipeline requiring management approval
    // ====================================================================

    @PostMapping("/{journalEntryId}/approve")
    public ResponseEntity<GlobeSuccessResponseBuilder> approveTransaction(
            @PathVariable UUID journalEntryId,
            @RequestBody ApprovalRequest request) throws Exception {

        log.info("Approving transaction: {} by user: {}", journalEntryId, request.getApproverId());

        JournalEntry result = transactionService.approveTransaction(
                journalEntryId,
                request.getApproverId(),
                request.getComments()
        );

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Transaction approved successfully",
                        mapToTransactionResponse(result)
                )
        );
    }

    // ====================================================================
    // UTILITY METHODS
    // ====================================================================

    private TransactionResponse mapToTransactionResponse(JournalEntry journalEntry) {
        TransactionResponse response = new TransactionResponse();
        response.setJournalEntryId(journalEntry.getId());
        response.setDescription(journalEntry.getDescription());
        response.setReferenceNumber(journalEntry.getReferenceNumber());
        response.setTransactionDateTime(journalEntry.getTransactionDateTime());
        response.setOrganisationId(journalEntry.getOrganisation().getOrganisationId());
        response.setTransactionLevel(journalEntry.getTransactionLevel().toString());
        response.setLineCount(journalEntry.getJournalEntryLines().size());

        if (journalEntry.getProject() != null) {
            response.setProjectId(journalEntry.getProject().getProjectId());
            response.setProjectName(journalEntry.getProject().getName());
        }

        // Calculate total amount (sum of debits)
        BigDecimal totalAmount = journalEntry.getJournalEntryLines().stream()
                .filter(line -> line.getDebitAmount() != null)
                .map(line -> line.getDebitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalAmount(totalAmount);

        return response;
    }
}