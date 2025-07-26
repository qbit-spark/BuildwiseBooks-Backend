package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalAction;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ApprovalActionResponse {
    private UUID instanceId;
    private UUID itemId;
    private ServiceType serviceName;
    private ApprovalStatus status;
    private int currentStep;
    private int totalSteps;
    private ApprovalAction actionTaken;
    private String actionBy;
    private boolean completed;
    private LocalDateTime completedAt;
}