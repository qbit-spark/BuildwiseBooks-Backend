package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionRequest;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;

import java.util.UUID;

public interface ApprovalActionService {
    ApprovalActionResponse takeApprovalAction(UUID organisationId, ServiceType serviceType, UUID itemId,
                                              ApprovalActionRequest request)
            throws ItemNotFoundException, AccessDeniedException;
}
