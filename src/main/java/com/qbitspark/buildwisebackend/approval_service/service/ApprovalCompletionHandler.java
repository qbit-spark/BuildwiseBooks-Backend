package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

public interface ApprovalCompletionHandler {
    void handleApprovalCompletion(ApprovalInstance instance) throws AccessDeniedException, ItemNotFoundException;
}