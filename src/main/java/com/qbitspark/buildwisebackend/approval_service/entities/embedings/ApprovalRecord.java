package com.qbitspark.buildwisebackend.approval_service.entities.embedings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalRecordStatus;
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
public class ApprovalRecord {

    private UUID approvalId;
    private UUID stepId;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String comments;
    private Integer revisionNumber;
    private ApprovalRecordStatus status;
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isActive() {
        return ApprovalRecordStatus.ACTIVE.equals(status);
    }

    public boolean isSuperseded() {
        return ApprovalRecordStatus.SUPERSEDED.equals(status);
    }

    public void markAsSuperseded() {
        this.status = ApprovalRecordStatus.SUPERSEDED;
    }

    public void markAsActive() {
        this.status = ApprovalRecordStatus.ACTIVE;
    }

    // Backward compatibility helpers
    public String getStatusAsString() {
        return status != null ? status.getValue() : null;
    }

    public void setStatusFromString(String statusString) {
        this.status = ApprovalRecordStatus.fromString(statusString);
    }
}