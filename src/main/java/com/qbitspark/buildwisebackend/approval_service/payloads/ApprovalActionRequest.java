package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalAction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalActionRequest {

    @NotNull(message = "Action is required")
    private ApprovalAction action;

    private String comments;
}