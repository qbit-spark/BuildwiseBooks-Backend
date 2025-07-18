package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;

import java.util.UUID;

public interface ApprovalIntegrationService {
    void submitForApproval(ServiceType serviceType, UUID itemId, UUID organisationId, UUID projectId)
            throws ItemNotFoundException, AccessDeniedException;
    void handleApprovalComplete(ServiceType serviceType, UUID itemId, boolean approved);
}