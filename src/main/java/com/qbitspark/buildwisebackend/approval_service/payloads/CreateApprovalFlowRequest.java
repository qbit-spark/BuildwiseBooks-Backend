package com.qbitspark.buildwisebackend.approval_service.payloads;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateApprovalFlowRequest {

    @NotNull(message = "Service name is required")
    private ServiceType serviceName;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Valid
    @NotEmpty(message = "At least one step is required")
    private List<CreateApprovalStepRequest> steps;
}