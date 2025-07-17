package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import lombok.Data;

import java.util.UUID;

@Data
public class ApprovalStepResponse {
    private UUID stepId;
    private int stepOrder;
    private ScopeType scopeType;
    private UUID roleId;
    private String roleName;
    private boolean isRequired;
}