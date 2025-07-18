package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;

public interface ApprovalCompletionHandler {
    void handleApprovalCompletion(ApprovalInstance instance);
}