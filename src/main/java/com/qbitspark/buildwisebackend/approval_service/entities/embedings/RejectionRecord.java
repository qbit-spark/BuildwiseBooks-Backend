package com.qbitspark.buildwisebackend.approval_service.entities.embedings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qbitspark.buildwisebackend.approval_service.enums.RejectionRecordStatus;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class RejectionRecord {

    private UUID rejectionId;
    private UUID stepId;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    private String rejectionReason;
    private Integer revisionNumber;
    private RejectionRecordStatus status;

    // Resolution tracking
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionComments;

    // Metadata
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isActive() {
        return RejectionRecordStatus.ACTIVE.equals(status);
    }

    public boolean isResolved() {
        return RejectionRecordStatus.RESOLVED.equals(status);
    }

    public void markAsResolved(String resolvedBy, String resolutionComments) {
        this.status = RejectionRecordStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
        this.resolutionComments = resolutionComments;
    }

    public void markAsActive() {
        this.status = RejectionRecordStatus.ACTIVE;
    }

    // Backward compatibility helpers
    public String getStatusAsString() {
        return status != null ? status.getValue() : null;
    }

    public void setStatusFromString(String statusString) {
        this.status = RejectionRecordStatus.fromString(statusString);
    }
}