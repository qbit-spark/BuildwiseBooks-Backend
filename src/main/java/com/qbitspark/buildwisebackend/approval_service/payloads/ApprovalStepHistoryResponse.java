package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalAction;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ApprovalStepHistoryResponse {
    private int stepOrder;
    private ScopeType scopeType;
    private UUID roleId;
    private String roleName;
    private boolean isRequired;
    private StepStatus status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String comments;
    private ApprovalAction action;
    private boolean canCurrentUserApprove;
}