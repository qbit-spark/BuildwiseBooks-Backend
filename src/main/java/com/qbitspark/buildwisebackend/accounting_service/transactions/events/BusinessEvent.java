package com.qbitspark.buildwisebackend.accounting_service.transactions.events;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public abstract class BusinessEvent {

    private UUID eventId;
    private UUID organisationId;
    private UUID projectId; // Optional
    private LocalDateTime eventDate;
    private String description;
    private String referenceNumber;
    private UUID createdBy;

    protected BusinessEvent() {
        this.eventId = UUID.randomUUID();
        this.eventDate = LocalDateTime.now();
    }

    public boolean isProjectLevel() {
        return this.projectId != null;
    }

    public boolean isValid() {
        return this.organisationId != null &&
                this.description != null &&
                !this.description.trim().isEmpty();
    }
}