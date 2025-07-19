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
    private String itemReference; // Invoice number, voucher number, etc.
    private String itemDescription; // Brief description
    private String projectName;
    private String clientName;

    // Timing Info
    private LocalDateTime submittedAt;
    private String submittedBy;
    private int daysWaiting; // How many days it's been waiting

    // Priority/Urgency
    private String priority; // "HIGH", "MEDIUM", "LOW"
    private boolean isOverdue; // Based on some business rule

    // Quick Actions
    private boolean canApprove;
    private boolean canReject;

}