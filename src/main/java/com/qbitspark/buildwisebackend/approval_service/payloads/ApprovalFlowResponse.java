package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ApprovalFlowResponse {
    private UUID flowId;
    private ServiceType serviceName;
    private String description;
    private UUID organisationId;
    private String organisationName;
    private boolean isActive;
    private int totalSteps;
    private List<ApprovalStepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}