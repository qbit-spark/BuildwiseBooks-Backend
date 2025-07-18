package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalHistoryResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.UUID;

public interface ApprovalHistoryService {
    ApprovalHistoryResponse getApprovalHistory(ServiceType serviceType, UUID itemId)
            throws ItemNotFoundException;
}