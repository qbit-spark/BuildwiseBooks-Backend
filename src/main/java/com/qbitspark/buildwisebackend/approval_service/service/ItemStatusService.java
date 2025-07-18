package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;

import java.util.UUID;

public interface ItemStatusService {
    void updateItemStatus(ServiceType serviceType, UUID itemId, boolean approved);
    void executePostApprovalActions(ServiceType serviceType, UUID itemId);
}