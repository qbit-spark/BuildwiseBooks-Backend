package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalPermissionService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalStatusService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalStatusServiceImpl implements ApprovalStatusService {

    private final ApprovalInstanceRepo approvalInstanceRepo;
    private final ApprovalStepInstanceRepo approvalStepInstanceRepo;
    private final ApprovalPermissionService permissionService;
    private final AccountRepo accountRepo;

    @Override
    public List<ApprovalInstance> getMyPendingApprovals() throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();

        List<ApprovalStepInstance> pendingSteps = approvalStepInstanceRepo.findByStatus(StepStatus.PENDING);

        return pendingSteps.stream()
                .filter(step -> permissionService.canUserApprove(currentUser, step))
                .map(ApprovalStepInstance::getApprovalInstance)
                .distinct()
                .toList();
    }

    @Override
    public ApprovalInstance getApprovalStatus(ServiceType serviceType, UUID itemId) throws ItemNotFoundException {
        return approvalInstanceRepo.findByServiceNameAndItemId(serviceType, itemId)
                .orElseThrow(() -> new ItemNotFoundException("No approval found for this item"));
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userName = userDetails.getUsername();

        return accountRepo.findByUserName(userName)
                .orElseThrow(() -> new ItemNotFoundException("User not found"));
    }
}