package com.qbitspark.buildwisebackend.approval_service.enums;

public enum StepStatus {
    WAITING,    // Step is waiting for previous steps
    PENDING,    // Step is ready for approval
    APPROVED,   // Step has been approved
    REJECTED,   // Step has been rejected
    SKIPPED     // Step was skipped (optional only)
}