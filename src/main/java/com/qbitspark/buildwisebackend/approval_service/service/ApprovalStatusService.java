package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ApprovalStatusService {
    List<ApprovalInstance> getMyPendingApprovals() throws ItemNotFoundException;
    ApprovalInstance getApprovalStatus(ServiceType serviceType, UUID itemId) throws ItemNotFoundException;
}