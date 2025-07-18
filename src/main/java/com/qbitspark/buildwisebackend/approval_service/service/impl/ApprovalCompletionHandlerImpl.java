package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalCompletionHandler;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalCompletionHandlerImpl implements ApprovalCompletionHandler {

    private final ApprovalIntegrationService approvalIntegrationService;

    @Override
    public void handleApprovalCompletion(ApprovalInstance instance) {
        boolean approved = instance.getStatus() == ApprovalStatus.APPROVED;

        approvalIntegrationService.handleApprovalComplete(
                instance.getServiceName(),
                instance.getItemId(),
                approved
        );
    }
}