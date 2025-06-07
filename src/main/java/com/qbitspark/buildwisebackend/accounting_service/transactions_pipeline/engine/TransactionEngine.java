package com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.engine;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntryLine;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.TransactionLevel;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.paylaods.JournalEntryLineRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.paylaods.JournalEntryRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEngine {

    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final OrganisationRepo organisationRepo;
    private final ProjectRepo projectRepo;

    /**
     * Convert a validated JournalEntryRequest into a complete JournalEntry entity
     * ready for database persistence.
     */
    public JournalEntry createJournalEntry(JournalEntryRequest request) throws ItemNotFoundException {

        log.debug("Creating journal entry for organisation: {}", request.getOrganisationId());

        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setTransactionDateTime(request.getTransactionDateTime());
        journalEntry.setDescription(request.getDescription());
        journalEntry.setReferenceNumber(request.getReferenceNumber());

        // Set organisation (required)
        OrganisationEntity organisation = organisationRepo.findById(request.getOrganisationId())
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found: " + request.getOrganisationId()));
        journalEntry.setOrganisation(organisation);

        // Set transaction level and project (if applicable)
        if (request.getProjectId() != null) {
            ProjectEntity project = projectRepo.findById(request.getProjectId())
                    .orElseThrow(() -> new ItemNotFoundException("Project not found: " + request.getProjectId()));

            journalEntry.setTransactionLevel(TransactionLevel.PROJECT);
            journalEntry.setProject(project);

            log.debug("Journal entry linked to project: {}", project.getName());
        } else {
            journalEntry.setTransactionLevel(TransactionLevel.ORGANISATION);
            log.debug("Journal entry created at organisation level");
        }

        // Create journal entry lines
        List<JournalEntryLine> lines = new ArrayList<>();
        for (JournalEntryLineRequest lineRequest : request.getJournalEntryLines()) {
            JournalEntryLine line = createJournalEntryLine(lineRequest, journalEntry);
            lines.add(line);
        }

        journalEntry.setJournalEntryLines(lines);

        log.debug("Created journal entry with {} lines", lines.size());
        return journalEntry;
    }

    /**
     * Create a single journal entry line from a request
     */
    private JournalEntryLine createJournalEntryLine(JournalEntryLineRequest request, JournalEntry journalEntry)
            throws ItemNotFoundException {

        JournalEntryLine line = new JournalEntryLine();
        line.setJournalEntry(journalEntry);
        line.setDebitAmount(request.getDebitAmount());
        line.setCreditAmount(request.getCreditAmount());
        line.setDescription(request.getDescription());

        // Find and set the account
        ChartOfAccounts account = chartOfAccountsRepo.findById(request.getAccountId())
                .orElseThrow(() -> new ItemNotFoundException("Account not found: " + request.getAccountId()));

        line.setAccount(account);

        log.debug("Created journal line for account: {} ({})", account.getName(), account.getAccountCode());
        return line;
    }

    /**
     * Generate a unique reference number if one wasn't provided
     */
    public String generateReferenceNumber(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    /**
     * Generate a reference number based on a transaction type
     */
    public String generateReferenceNumber(TransactionLevel level, UUID organisationId) {
        String prefix = switch (level) {
            case ORGANISATION -> "ORG";
            case PROJECT -> "PROJ";
            case TASK -> "TASK";
        };

        return prefix + "-" + organisationId.toString().substring(0, 8).toUpperCase() + "-" + System.currentTimeMillis();
    }
}