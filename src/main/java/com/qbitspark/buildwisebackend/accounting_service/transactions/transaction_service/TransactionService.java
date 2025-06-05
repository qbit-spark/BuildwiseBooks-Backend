package com.qbitspark.buildwisebackend.accounting_service.transactions.transaction_service;

import com.qbitspark.buildwisebackend.accounting_service.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.transactions.events.BusinessEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryRequest;

import java.util.UUID;

/**
 * Main service interface for processing accounting transactions.
 *
 * This is the public API that controllers and other services use to create
 * accounting transactions. It orchestrates all 6 layers of our transaction
 * architecture to convert business events into saved journal entries.
 */
public interface TransactionService {

    /**
     * Process a business event through the complete transaction pipeline.
     *
     * This is the main method that handles the complete flow:
     * 1. Business Event (what happened)
     * 2. Interpreter (convert to accounting language)
     * 3. Engine (create entities)
     * 4. Validator (ensure correctness)
     * 5. Approval (if required)
     * 6. Save (persist to database)
     *
     * @param event The business event to process
     * @return Saved journal entry
     * @throws Exception if validation fails or any step in the pipeline fails
     */
    JournalEntry processBusinessEvent(BusinessEvent event) throws Exception;

    /**
     * Create a journal entry directly from a request (bypassing business events).
     *
     * Use this when you need direct control over the journal entry creation
     * or when integrating with external systems that provide accounting data directly.
     *
     * @param request The journal entry request
     * @return Saved journal entry
     * @throws Exception if validation fails or creation fails
     */
    JournalEntry createJournalEntry(JournalEntryRequest request) throws Exception;

    /**
     * Approve a transaction that requires approval.
     *
     * For transactions that need management approval before being posted.
     *
     * @param journalEntryId The journal entry to approve
     * @param approverId Who is approving it
     * @param comments Approval comments
     * @return Updated journal entry
     * @throws Exception if approval fails
     */
    JournalEntry approveTransaction(UUID journalEntryId, UUID approverId, String comments) throws Exception;
}