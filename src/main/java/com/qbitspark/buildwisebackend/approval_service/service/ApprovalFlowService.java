package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.payloads.CreateApprovalFlowRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ApprovalFlowService {

    ApprovalFlow createApprovalFlow(UUID organisationId, CreateApprovalFlowRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    ApprovalFlow updateApprovalFlow(UUID flowId, CreateApprovalFlowRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    ApprovalFlow getApprovalFlowByService(UUID organisationId, ServiceType serviceName)
            throws ItemNotFoundException;

    List<ApprovalFlow> getAllApprovalFlows(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    void deleteApprovalFlow(UUID flowId)
            throws ItemNotFoundException, AccessDeniedException;
}