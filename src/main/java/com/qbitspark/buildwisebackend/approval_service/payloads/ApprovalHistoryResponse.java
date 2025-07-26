package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ApprovalHistoryResponse {
    private UUID instanceId;
    private ServiceType serviceName;
    private UUID itemId;
    private ApprovalStatus status;
    private int currentStep;
    private int totalSteps;
    private String submittedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private boolean canCurrentUserApprove;
    private List<ApprovalStepHistoryResponse> steps;
}