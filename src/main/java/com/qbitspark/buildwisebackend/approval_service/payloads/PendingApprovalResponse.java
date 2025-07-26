package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
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
public class PendingApprovalResponse {

    // Approval Instance Info
    private UUID instanceId;
    private UUID itemId;
    private ServiceType serviceName;
    private ApprovalStatus status;

    // Step Info (where I need to approve)
    private int currentStep;
    private int totalSteps;
    private int myStepOrder;
    private ScopeType myScopeType;
    private String myRoleName;

    // Item Context (what am I approving?)
    private String itemReference;
    private String itemDescription;
    private String projectName;
    private String clientName;

    // Timing Info
    private LocalDateTime submittedAt;
    private String submittedBy;
    private int daysWaiting;

    // Priority/Urgency
    private String priority;
    private boolean isOverdue;

    // REJECTION CONTEXT - Why did this come back?
    private boolean hasRejectionHistory;
    private RejectionContext rejectionContext;

    // Quick Actions
    private boolean canApprove;
    private boolean canReject;

    // REJECTION CONTEXT CLASS
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RejectionContext {
        private boolean isComingBackFromRejection;
        private String rejectedByRole;
        private String rejectedByUser;
        private LocalDateTime rejectedAt;
        private String rejectionReason;
        private int rejectedFromStep;
        private String rejectedFromStepName;
        private int timesRejected;
        private String contextMessage;
        private String actionRequired;
    }
}