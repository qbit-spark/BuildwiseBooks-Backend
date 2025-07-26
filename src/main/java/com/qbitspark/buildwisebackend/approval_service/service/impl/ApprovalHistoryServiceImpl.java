package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.embedings.ApprovalRecord;
import com.qbitspark.buildwisebackend.approval_service.entities.embedings.RejectionRecord;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalHistoryResponse;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalHistorySummary;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalStepHistoryResponse;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalHistoryService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalPermissionService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ResourceNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.OrgMemberRoleRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalHistoryServiceImpl implements ApprovalHistoryService {

    private final ApprovalInstanceRepo approvalInstanceRepo;
    private final ApprovalStepInstanceRepo approvalStepInstanceRepo;
    private final ApprovalPermissionService permissionService;
    private final AccountRepo accountRepo;
    private final OrgMemberRoleRepo orgMemberRoleRepo;
    private final ProjectTeamRoleRepo projectTeamRoleRepo;

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

        // Basic step information
        stepResponse.setStepOrder(step.getStepOrder());
        stepResponse.setScopeType(step.getScopeType());
        stepResponse.setRoleId(step.getRoleId());

        //Set role based on level
        String roleName = getRoleName(step.getRoleId(), step.getScopeType());
        stepResponse.setRoleName(roleName);
        stepResponse.setRequired(step.isRequired());
        stepResponse.setStatus(step.getStatus());
        stepResponse.setComments(step.getComments());
        stepResponse.setApprovedAt(step.getApprovedAt());
        stepResponse.setAction(step.getAction());


        // Current approver info
        if (step.getApprovedBy() != null) {
            AccountEntity approver = accountRepo.findById(step.getApprovedBy()).orElse(null);
            stepResponse.setApprovedBy(approver != null ? approver.getUserName() : "Unknown");
        }

        // Permission check
        if (step.getStatus() == StepStatus.PENDING) {
            stepResponse.setCanCurrentUserApprove(permissionService.canUserApprove(currentUser, step));
        } else {
            stepResponse.setCanCurrentUserApprove(false);
        }

        // Add complete history
        stepResponse.setApprovalHistory(step.getApprovalHistory());
        stepResponse.setRejectionHistory(step.getRejectionHistory());

        //Build history summary
        ApprovalHistorySummary summary = buildHistorySummary(step);
        stepResponse.setHistorySummary(summary);

        //Set revision info
        stepResponse.setCurrentRevision(step.getNextRevisionNumber() - 1);
        stepResponse.setRevision(step.getTotalApprovals() > 0 || step.getTotalRejections() > 0);

        //Build a user context message
        String userMessage = buildUserContextMessage(step, currentUser);
        stepResponse.setUserMessage(userMessage);

        String actionRequired = determineActionRequired(step, currentUser);
        stepResponse.setActionRequired(actionRequired);

        return stepResponse;
    }

    private ApprovalHistorySummary buildHistorySummary(ApprovalStepInstance step) {
        ApprovalHistorySummary summary = new ApprovalHistorySummary();

        summary.setTotalApprovals(step.getTotalApprovals());
        summary.setTotalRejections(step.getTotalRejections());
        summary.setCurrentRevision(step.getNextRevisionNumber() - 1);
        summary.setHasActiveRejections(step.hasActiveRejections());

        // Count active/resolved rejections
        long activeRejections = step.getRejectionHistory().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .count();
        long resolvedRejections = step.getRejectionHistory().stream()
                .filter(r -> "RESOLVED".equals(r.getStatus()))
                .count();

        summary.setActiveRejections((int) activeRejections);
        summary.setResolvedRejections((int) resolvedRejections);

        // Count superseded approvals
        long supersededApprovals = step.getApprovalHistory().stream()
                .filter(a -> "SUPERSEDED".equals(a.getStatus()))
                .count();
        summary.setSupersededApprovals((int) supersededApprovals);

        // Determine last action
        if (step.getAction() != null) {
            summary.setLastAction(step.getAction().name());
            summary.setLastActionAt(step.getApprovedAt());

            if (step.getApprovedBy() != null) {
                AccountEntity lastActor = accountRepo.findById(step.getApprovedBy()).orElse(null);
                summary.setLastActionBy(lastActor != null ? lastActor.getUserName() : "Unknown");
            }
        }

        return summary;
    }

    private String buildUserContextMessage(ApprovalStepInstance step, AccountEntity currentUser) {
        if (step.getStatus() == StepStatus.PENDING) {
            // Check if user previously approved this step
            boolean userPreviouslyApproved = step.getApprovalHistory().stream()
                    .anyMatch(approval -> currentUser.getUserName().equals(approval.getApprovedBy()));

            if (userPreviouslyApproved) {
                ApprovalRecord lastApproval = step.getApprovalHistory().stream()
                        .filter(approval -> currentUser.getUserName().equals(approval.getApprovedBy()))
                        .reduce((first, second) -> second) // Get last
                        .orElse(null);

                if (lastApproval != null) {
                    return String.format("You previously approved this on %s with comment: '%s'",
                            lastApproval.getApprovedAt().toLocalDate(),
                            lastApproval.getComments());
                }
            }

            // Check if there are active rejections
            if (step.hasActiveRejections()) {
                RejectionRecord latestRejection = step.getLatestRejection();
                if (latestRejection != null) {
                    return String.format("This step came back due to rejection: '%s' by %s",
                            latestRejection.getRejectionReason(),
                            latestRejection.getRejectedBy());
                }
            }

            return "This step is awaiting your approval";
        }

        return "This step has been completed";
    }

    private String determineActionRequired(ApprovalStepInstance step, AccountEntity currentUser) {
        if (step.getStatus() == StepStatus.PENDING) {
            if (permissionService.canUserApprove(currentUser, step)) {
                if (step.hasActiveRejections()) {
                    return "REVIEW_AND_APPROVE"; // Step came back due to rejection
                } else {
                    return "APPROVE"; // Normal approval
                }
            } else {
                return "WAITING_FOR_APPROVER"; // Someone else needs to approve
            }
        }

        return "COMPLETED"; // Step is already done
    }

    private String getRoleName(UUID roleId, ScopeType scopeType) {
        try {
            switch (scopeType) {
                case ORGANIZATION -> {
                    Optional<OrgMemberRoleEntity> orgRole = orgMemberRoleRepo.findById(roleId);
                    return orgRole.map(OrgMemberRoleEntity::getRoleName).orElse("Unknown Role");
                }
                case PROJECT -> {
                    Optional<ProjectTeamRoleEntity> projectRole = projectTeamRoleRepo.findById(roleId);
                    return projectRole.map(ProjectTeamRoleEntity::getRoleName).orElse("Unknown Role");
                }
                default -> {
                    return "Unknown Role";
                }
            }
        } catch (Exception e) {
            return "Unknown Role";
        }
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userName = userDetails.getUsername();

        return accountRepo.findByUserName(userName)
                .orElseThrow(() -> new ItemNotFoundException("User not found"));
    }

}