package com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class JournalEntryRequest {

    private UUID organisationId;
    private UUID projectId;
    private String description;
    private String referenceNumber;
    private LocalDateTime transactionDateTime = LocalDateTime.now();
    private List<JournalEntryLineRequest> journalEntryLines;

    public JournalEntryRequest() {}

    public JournalEntryRequest(UUID organisationId, String description) {
        this.organisationId = organisationId;
        this.description = description;
    }

    public JournalEntryRequest(UUID organisationId, UUID projectId, String description) {
        this.organisationId = organisationId;
        this.projectId = projectId;
        this.description = description;
    }

    public boolean hasLines() {
        return journalEntryLines != null && !journalEntryLines.isEmpty();
    }

    public int getLineCount() {
        return hasLines() ? journalEntryLines.size() : 0;
    }

    public boolean isProjectLevel() {
        return projectId != null;
    }
}