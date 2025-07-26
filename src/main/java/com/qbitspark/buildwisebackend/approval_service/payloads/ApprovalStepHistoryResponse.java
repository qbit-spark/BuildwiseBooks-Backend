package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.entities.embedings.ApprovalRecord;
import com.qbitspark.buildwisebackend.approval_service.entities.embedings.RejectionRecord;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalAction;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ApprovalStepHistoryResponse {

    // Basic step information
    private int stepOrder;
    private ScopeType scopeType;
    private UUID roleId;
    private String roleName;
    private boolean isRequired;
    private StepStatus status;

    // Current state
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String comments;
    private ApprovalAction action;
    private boolean canCurrentUserApprove;

    // Complete history tracking
    private List<ApprovalRecord> approvalHistory;
    private List<RejectionRecord> rejectionHistory;
    private ApprovalHistorySummary historySummary;

    // User context
    private String userMessage; // "You previously approved this on Jan 15" or "CEO rejected due to budget concerns"
    private String actionRequired; // "Please review and approve" or "Already completed"

    // Revision tracking
    private Integer currentRevision;
    private boolean isRevision; // True if this step has been through approval/rejection cycles
}