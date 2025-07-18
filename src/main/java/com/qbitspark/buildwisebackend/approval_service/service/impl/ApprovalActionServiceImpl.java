package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionRequest;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionResponse;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalActionService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalPermissionService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalWorkflowService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalActionServiceImpl implements ApprovalActionService {

    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApprovalInstanceRepo approvalInstanceRepo;
    private final ApprovalStepInstanceRepo approvalStepInstanceRepo;
    private final ApprovalPermissionService permissionService;
    private final AccountRepo accountRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;

    @Override
    public ApprovalActionResponse takeApprovalAction(UUID organisationId, ServiceType serviceType, UUID itemId,
                                                     ApprovalActionRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationMemberAccess(currentUser, organisation);

        ApprovalInstance instance = approvalInstanceRepo.findByServiceNameAndItemId(serviceType, itemId)
                .orElseThrow(() -> new ItemNotFoundException("No approval found for this item"));

        ApprovalStepInstance currentStep = approvalStepInstanceRepo
                .findByApprovalInstanceAndStepOrder(instance, instance.getCurrentStepOrder())
                .orElseThrow(() -> new ItemNotFoundException("Current step not found"));

        if (currentStep.getStatus() != StepStatus.PENDING) {
            throw new AccessDeniedException("This step is not pending approval");
        }

        if (!permissionService.canUserApprove(currentUser, currentStep)) {
            throw new AccessDeniedException("You do not have permission to approve this step");
        }

        ApprovalInstance updatedInstance = approvalWorkflowService.processApprovalAction(
                instance.getInstanceId(),
                request.getAction(),
                request.getComments()
        );

        ApprovalActionResponse response = new ApprovalActionResponse();
        response.setInstanceId(updatedInstance.getInstanceId());
        response.setItemId(updatedInstance.getItemId());
        response.setServiceName(updatedInstance.getServiceName());
        response.setStatus(updatedInstance.getStatus());
        response.setCurrentStep(updatedInstance.getCurrentStepOrder());
        response.setTotalSteps(updatedInstance.getTotalSteps());
        response.setActionTaken(request.getAction());
        response.setActionBy(currentUser.getUserName());

        if (updatedInstance.getCompletedAt() != null) {
            response.setCompleted(true);
            response.setCompletedAt(updatedInstance.getCompletedAt());
        } else {
            response.setCompleted(false);
        }

        return response;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userName = userDetails.getUsername();

        return accountRepo.findByUserName(userName)
                .orElseThrow(() -> new ItemNotFoundException("User not found"));
    }

    private void validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

    }
}