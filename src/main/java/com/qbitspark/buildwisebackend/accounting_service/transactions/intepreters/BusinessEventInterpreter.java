package com.qbitspark.buildwisebackend.accounting_service.transactions.intepreters;


import com.qbitspark.buildwisebackend.accounting_service.transactions.events.BusinessEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryRequest;

/**
 * Interface for converting business events into journal entry requests.
 *
 * This is the heart of Layer 2 in our transaction architecture.
 * Each business event type (Invoice, Expense, etc.) will have its own interpreter
 * that knows how to convert that specific business event into the correct
 * accounting journal entries.
 */
public interface BusinessEventInterpreter<T extends BusinessEvent> {

    /**
     * Convert a business event into a journal entry request.
     *
     * This is where the business logic gets translated into accounting logic.
     * For example:
     * - Invoice event becomes: Debit A/R, Credit Revenue + Tax
     * - Expense event becomes: Debit Expense, Credit Cash/A/P
     *
     * @param event The business event to interpret
     * @return JournalEntryRequest with all the debit/credit lines needed
     */
    JournalEntryRequest interpret(T event);

    /**
     * Check if this interpreter can handle a specific type of business event.
     *
     * Used by the transaction service to find the right interpreter
     * for each business event type.
     *
     * @param eventType The class type of the business event
     * @return true if this interpreter can handle this event type
     */
    boolean canHandle(Class<? extends BusinessEvent> eventType);
}