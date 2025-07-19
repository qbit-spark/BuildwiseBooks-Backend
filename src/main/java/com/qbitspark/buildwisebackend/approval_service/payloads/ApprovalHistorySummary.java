package com.qbitspark.buildwisebackend.approval_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistorySummary {

    private Integer totalApprovals;
    private Integer totalRejections;
    private Integer currentRevision;
    private String lastAction; // "APPROVED" or "REJECTED"
    private LocalDateTime lastActionAt;
    private String lastActionBy;
    private boolean hasActiveRejections;
    private boolean hasActiveApprovals;

    // Summary statistics
    private Integer activeRejections;
    private Integer resolvedRejections;
    private Integer supersededApprovals;

    // User context
    private String nextActionRequired; // "APPROVE", "INVESTIGATE", "REVISE"
    private String contextMessage; // Human-readable summary
}