package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalAction;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ApprovalWorkflowService {

    ApprovalInstance startApprovalWorkflow(ServiceType serviceName, UUID itemId, UUID organisationId, UUID contextProjectId)
            throws ItemNotFoundException, AccessDeniedException;

    ApprovalInstance processApprovalAction(UUID instanceId, ApprovalAction action, String comments)
            throws ItemNotFoundException, AccessDeniedException;

    ApprovalInstance getApprovalInstanceByItem(ServiceType serviceName, UUID itemId)
            throws ItemNotFoundException;

    List<ApprovalInstance> getMySubmittedApprovals()
            throws ItemNotFoundException;

    List<ApprovalInstance> getPendingApprovalsForUser()
            throws ItemNotFoundException;

    boolean canUserApproveStep(UUID instanceId, int stepOrder)
            throws ItemNotFoundException;
}