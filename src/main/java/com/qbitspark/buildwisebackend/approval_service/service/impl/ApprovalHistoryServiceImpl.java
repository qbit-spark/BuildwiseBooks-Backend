package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalHistoryResponse;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalStepHistoryResponse;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalHistoryService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalPermissionService;
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
public class ApprovalHistoryServiceImpl implements ApprovalHistoryService {

    private final ApprovalInstanceRepo approvalInstanceRepo;
    private final ApprovalStepInstanceRepo approvalStepInstanceRepo;
    private final ApprovalPermissionService permissionService;
    private final AccountRepo accountRepo;

    @Override
    public ApprovalHistoryResponse getApprovalHistory(ServiceType serviceType, UUID itemId)
            throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ApprovalInstance instance = approvalInstanceRepo.findByServiceNameAndItemId(serviceType, itemId)
                .orElseThrow(() -> new ItemNotFoundException("No approval found for this item"));

        List<ApprovalStepInstance> steps = approvalStepInstanceRepo
                .findByApprovalInstanceOrderByStepOrderAsc(instance);

        ApprovalHistoryResponse response = new ApprovalHistoryResponse();
        response.setInstanceId(instance.getInstanceId());
        response.setServiceName(instance.getServiceName());
        response.setItemId(instance.getItemId());
        response.setStatus(instance.getStatus());
        response.setCurrentStep(instance.getCurrentStepOrder());
        response.setTotalSteps(instance.getTotalSteps());
        response.setStartedAt(instance.getStartedAt());
        response.setCompletedAt(instance.getCompletedAt());

        AccountEntity submitter = accountRepo.findById(instance.getSubmittedBy()).orElse(null);
        response.setSubmittedBy(submitter != null ? submitter.getUserName() : "Unknown");

        List<ApprovalStepHistoryResponse> stepHistory = steps.stream()
                .map(step -> buildStepHistory(step, currentUser))
                .toList();

        response.setSteps(stepHistory);

        if (instance.getCurrentStepOrder() <= steps.size()) {
            ApprovalStepInstance currentStep = steps.get(instance.getCurrentStepOrder() - 1);
            if (currentStep.getStatus() == StepStatus.PENDING) {
                response.setCanCurrentUserApprove(permissionService.canUserApprove(currentUser, currentStep));
            }
        }

        return response;
    }

    private ApprovalStepHistoryResponse buildStepHistory(ApprovalStepInstance step, AccountEntity currentUser) {
        ApprovalStepHistoryResponse stepResponse = new ApprovalStepHistoryResponse();
        stepResponse.setStepOrder(step.getStepOrder());
        stepResponse.setScopeType(step.getScopeType());
        stepResponse.setRoleId(step.getRoleId());
        stepResponse.setRequired(step.isRequired());
        stepResponse.setStatus(step.getStatus());
        stepResponse.setComments(step.getComments());
        stepResponse.setApprovedAt(step.getApprovedAt());
        stepResponse.setAction(step.getAction());

        if (step.getApprovedBy() != null) {
            AccountEntity approver = accountRepo.findById(step.getApprovedBy()).orElse(null);
            stepResponse.setApprovedBy(approver != null ? approver.getUserName() : "Unknown");
        }

        if (step.getStatus() == StepStatus.PENDING) {
            stepResponse.setCanCurrentUserApprove(permissionService.canUserApprove(currentUser, step));
        } else {
            stepResponse.setCanCurrentUserApprove(false);
        }

        return stepResponse;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userName = userDetails.getUsername();

        return accountRepo.findByUserName(userName)
                .orElseThrow(() -> new ItemNotFoundException("User not found"));
    }
}