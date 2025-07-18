package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionRequest;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.UUID;

public interface ApprovalActionService {
    ApprovalActionResponse takeApprovalAction(ServiceType serviceType, UUID itemId,
                                              ApprovalActionRequest request)
            throws ItemNotFoundException, AccessDeniedException;
}
