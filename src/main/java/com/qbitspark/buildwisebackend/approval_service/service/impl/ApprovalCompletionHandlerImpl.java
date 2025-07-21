package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalCompletionHandler;
import com.qbitspark.buildwisebackend.approval_service.service.ItemStatusService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalCompletionHandlerImpl implements ApprovalCompletionHandler {

    private final ItemStatusService itemStatusService;

    @Override
    public void handleApprovalCompletion(ApprovalInstance instance) throws AccessDeniedException, ItemNotFoundException {
        boolean approved = instance.getStatus() == ApprovalStatus.APPROVED;

        // Update item status via shared service
        itemStatusService.updateItemStatus(
                instance.getServiceName(),
                instance.getItemId(),
                approved
        );

        // Execute post-approval actions if approved
        if (approved) {
            itemStatusService.executePostApprovalActions(
                    instance.getServiceName(),
                    instance.getItemId()
            );
        }
    }
}