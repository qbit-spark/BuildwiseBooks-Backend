package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateApprovalStepRequest {

    @Min(value = 1, message = "Step order must be at least 1")
    private int stepOrder;

    @NotNull(message = "Scope type is required")
    private ScopeType scopeType;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    private boolean isRequired = true;
}