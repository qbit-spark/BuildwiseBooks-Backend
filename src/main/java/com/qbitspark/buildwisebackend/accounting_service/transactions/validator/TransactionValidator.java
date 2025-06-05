package com.qbitspark.buildwisebackend.accounting_service.transactions.validator;

import com.qbitspark.buildwisebackend.accounting_service.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.entity.JournalEntryLine;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryLineRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionValidator {

    /**
     * Validate a journal entry request before it's processed
     */
    public ValidationResult validateJournalEntry(JournalEntryRequest request) {
        List<String> errors = new ArrayList<>();

        // Basic field validation
        validateBasicFields(request, errors);

        // Line validation
        if (request.getJournalEntryLines() != null) {
            validateJournalEntryLines(request.getJournalEntryLines(), errors);
            validateDebitCreditBalance(request.getJournalEntryLines(), errors);
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate a journal entry entity before it's saved to database
     */
    public ValidationResult validateJournalEntry(JournalEntry journalEntry) {
        List<String> errors = new ArrayList<>();

        // Validate accounts are postable and active
        validateAccountProperties(journalEntry, errors);

        // Re-validate debit/credit balance on entity
        validateEntityDebitCreditBalance(journalEntry, errors);

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void validateBasicFields(JournalEntryRequest request, List<String> errors) {
        if (request.getOrganisationId() == null) {
            errors.add("Organisation ID is required");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            errors.add("Description is required");
        }

        if (request.getJournalEntryLines() == null || request.getJournalEntryLines().isEmpty()) {
            errors.add("At least one journal entry line is required");
        }

        if (request.getTransactionDateTime() == null) {
            errors.add("Transaction date/time is required");
        }
    }

    private void validateJournalEntryLines(List<JournalEntryLineRequest> lines, List<String> errors) {
        for (int i = 0; i < lines.size(); i++) {
            JournalEntryLineRequest line = lines.get(i);
            validateSingleLine(line, i + 1, errors);
        }
    }

    private void validateSingleLine(JournalEntryLineRequest line, int lineNumber, List<String> errors) {
        String linePrefix = "Line " + lineNumber + ": ";

        if (line.getAccountId() == null) {
            errors.add(linePrefix + "Account ID is required");
        }

        boolean hasDebit = line.getDebitAmount() != null && line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasCredit = line.getCreditAmount() != null && line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;

        if (!hasDebit && !hasCredit) {
            errors.add(linePrefix + "Either debit or credit amount must be greater than zero");
        }

        if (hasDebit && hasCredit) {
            errors.add(linePrefix + "Cannot have both debit and credit amounts on the same line");
        }

        if (hasDebit && line.getDebitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(linePrefix + "Debit amount must be positive");
        }

        if (hasCredit && line.getCreditAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(linePrefix + "Credit amount must be positive");
        }

        if (line.getDescription() == null || line.getDescription().trim().isEmpty()) {
            errors.add(linePrefix + "Line description is required");
        }
    }

    private void validateDebitCreditBalance(List<JournalEntryLineRequest> lines, List<String> errors) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalEntryLineRequest line : lines) {
            if (line.getDebitAmount() != null) {
                totalDebits = totalDebits.add(line.getDebitAmount());
            }
            if (line.getCreditAmount() != null) {
                totalCredits = totalCredits.add(line.getCreditAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            errors.add("Total debits (" + totalDebits + ") must equal total credits (" + totalCredits + ")");
        }
    }

    private void validateAccountProperties(JournalEntry journalEntry, List<String> errors) {
        for (JournalEntryLine line : journalEntry.getJournalEntryLines()) {
            if (line.getAccount() == null) {
                errors.add("Journal entry line is missing account reference");
                continue;
            }

            if (!line.getAccount().getIsPostable()) {
                errors.add("Account '" + line.getAccount().getName() + "' (" +
                        line.getAccount().getAccountCode() + ") is not postable");
            }

            if (!line.getAccount().getIsActive()) {
                errors.add("Account '" + line.getAccount().getName() + "' (" +
                        line.getAccount().getAccountCode() + ") is not active");
            }
        }
    }

    private void validateEntityDebitCreditBalance(JournalEntry journalEntry, List<String> errors) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalEntryLine line : journalEntry.getJournalEntryLines()) {
            if (line.getDebitAmount() != null) {
                totalDebits = totalDebits.add(line.getDebitAmount());
            }
            if (line.getCreditAmount() != null) {
                totalCredits = totalCredits.add(line.getCreditAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            errors.add("Journal entry debits (" + totalDebits + ") must equal credits (" + totalCredits + ")");
        }
    }

    /**
     * Result object that contains validation outcome and any error messages
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}