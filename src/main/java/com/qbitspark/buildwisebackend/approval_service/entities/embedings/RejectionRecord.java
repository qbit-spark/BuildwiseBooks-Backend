package com.qbitspark.buildwisebackend.approval_service.entities.embedings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectionRecord {

    private UUID rejectionId;
    private UUID stepId;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String rejectionReason;
    private Integer revisionNumber;
    private String status; // "ACTIVE", "RESOLVED"

    // Resolution tracking
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionComments;

    // Metadata
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }

    public void markAsResolved(String resolvedBy, String resolutionComments) {
        this.status = "RESOLVED";
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
        this.resolutionComments = resolutionComments;
    }

    public void markAsActive() {
        this.status = "ACTIVE";
    }
}