package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalIntegrationService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalFlowService;
import com.qbitspark.buildwisebackend.approval_service.service.ItemStatusService;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalIntegrationServiceImpl implements ApprovalIntegrationService {

    private final ApprovalFlowService approvalFlowService;
    private final ItemStatusService itemStatusService;

    @Override
    public void submitForApproval(ServiceType serviceType, UUID itemId, UUID organisationId, UUID projectId) {

        // Update item status to PENDING_APPROVAL
        itemStatusService.updateItemStatus(serviceType, itemId, false);
    }

    @Override
    public void handleApprovalComplete(ServiceType serviceType, UUID itemId, boolean approved) throws AccessDeniedException, ItemNotFoundException {
        // Update item status based on an approval result
        itemStatusService.updateItemStatus(serviceType, itemId, approved);

        // Execute post-approval actions if approved
        if (approved) {
            itemStatusService.executePostApprovalActions(serviceType, itemId);
        }
    }
}