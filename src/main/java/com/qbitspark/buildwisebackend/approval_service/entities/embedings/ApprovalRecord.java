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
public class ApprovalRecord {

    private UUID approvalId;
    private UUID stepId;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String comments;
    private Integer revisionNumber;
    private String status; // "ACTIVE", "SUPERSEDED"
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isSuperseded() {
        return "SUPERSEDED".equals(status);
    }

    public void markAsSuperseded() {
        this.status = "SUPERSEDED";
    }

    public void markAsActive() {
        this.status = "ACTIVE";
    }
}