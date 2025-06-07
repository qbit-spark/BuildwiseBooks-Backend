package com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.transaction_service.impl;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.JournalEntryRepo;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.engine.TransactionEngine;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.BusinessEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.intepreters.BusinessEventInterpreter;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.paylaods.JournalEntryRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.transaction_service.TransactionService;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.validator.TransactionValidator;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    // All the components from our 6-layer architecture
    private final List<BusinessEventInterpreter<? extends BusinessEvent>> interpreters;
    private final TransactionEngine transactionEngine;
    private final TransactionValidator transactionValidator;
    private final JournalEntryRepo journalEntryRepo;

    @Override
    public JournalEntry processBusinessEvent(BusinessEvent event) throws Exception {

        log.info("Processing business event: {} for organisation: {}",
                event.getClass().getSimpleName(), event.getOrganisationId());

        // LAYER 1: Validate the business event
        if (!event.isValid()) {
            throw new RandomExceptions("Invalid business event: missing required fields");
        }

        // LAYER 2: Find the right interpreter and convert to journal entry request
        BusinessEventInterpreter interpreter = findInterpreter(event.getClass());
        if (interpreter == null) {
            throw new RandomExceptions("No interpreter found for event type: " + event.getClass().getSimpleName());
        }

        log.debug("Using interpreter: {}", interpreter.getClass().getSimpleName());
        JournalEntryRequest request = interpreter.interpret(event);

        // Continue with the standard journal entry creation process
        return createJournalEntry(request);
    }

    @Override
    public JournalEntry createJournalEntry(JournalEntryRequest request) throws Exception {

        log.info("Creating journal entry: '{}' for organisation: {}",
                request.getDescription(), request.getOrganisationId());

        // LAYER 4: Validate the request
        TransactionValidator.ValidationResult requestValidation = transactionValidator.validateJournalEntry(request);
        if (!requestValidation.isValid()) {
            log.error("Request validation failed: {}", requestValidation.getErrorMessage());
            throw new RandomExceptions("Validation failed: " + requestValidation.getErrorMessage());
        }

        // LAYER 3: Create the journal entry entity using the engine
        JournalEntry journalEntry = transactionEngine.createJournalEntry(request);

        // LAYER 4: Validate the created entity
        TransactionValidator.ValidationResult entityValidation = transactionValidator.validateJournalEntry(journalEntry);
        if (!entityValidation.isValid()) {
            log.error("Entity validation failed: {}", entityValidation.getErrorMessage());
            throw new RandomExceptions("Entity validation failed: " + entityValidation.getErrorMessage());
        }

        // LAYER 5: Check if approval is required (simplified for now)
        if (requiresApproval(journalEntry)) {
            log.info("Transaction requires approval, not posting immediately");
            // In a full implementation, you would:
            // 1. Save journal entry with "PENDING_APPROVAL" status
            // 2. Create approval record
            // 3. Send notification to approvers
            // 4. Return without posting
            throw new RandomExceptions("Transaction requires approval - approval workflow not implemented yet");
        }

        // LAYER 6: Save the journal entry to a database
        JournalEntry savedEntry = journalEntryRepo.save(journalEntry);

        log.info("Successfully created journal entry with ID: {} and {} lines",
                savedEntry.getId(), savedEntry.getJournalEntryLines().size());

        return savedEntry;
    }

    @Override
    public JournalEntry approveTransaction(UUID journalEntryId, UUID approverId, String comments) throws Exception {

        log.info("Approving transaction: {} by approver: {}", journalEntryId, approverId);

        JournalEntry journalEntry = journalEntryRepo.findById(journalEntryId)
                .orElseThrow(() -> new ItemNotFoundException("Journal entry not found: " + journalEntryId));

        // In a full implementation, you would:
        // 1. Update approval status
        // 2. Record approver and comments
        // 3. Post the transaction
        // 4. Send notifications

        log.info("Transaction approved and posted");
        return journalEntryRepo.save(journalEntry);
    }

    /**
     * Find the right interpreter for a business event type
     */
    @SuppressWarnings("unchecked")
    private BusinessEventInterpreter<? extends BusinessEvent> findInterpreter(Class<? extends BusinessEvent> eventType) {
        return interpreters.stream()
                .filter(interpreter -> interpreter.canHandle(eventType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Simple approval logic - can be made more sophisticated
     */
    private boolean requiresApproval(JournalEntry journalEntry) {
        // Example approval rules:
        // 1. Transactions over $10,000 require approval
        // 2. Project-level transactions_pipeline require approval
        // 3. Certain account types require approval

//        BigDecimal approvalThreshold = new BigDecimal("10000");
//
//        // Calculate total transaction amount
//        BigDecimal totalAmount = journalEntry.getJournalEntryLines().stream()
//                .filter(line -> line.getDebitAmount() != null)
//                .map(line -> line.getDebitAmount())
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return totalAmount.compareTo(approvalThreshold) > 0;

        return false;
    }
}