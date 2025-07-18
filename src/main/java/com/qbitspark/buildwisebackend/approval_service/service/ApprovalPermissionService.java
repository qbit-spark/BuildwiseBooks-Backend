package com.qbitspark.buildwisebackend.approval_service.service;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;

public interface ApprovalPermissionService {
    boolean canUserApprove(AccountEntity user, ApprovalStepInstance stepInstance);
}