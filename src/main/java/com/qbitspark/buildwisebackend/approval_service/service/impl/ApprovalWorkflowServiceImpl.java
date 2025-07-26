package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStep;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalAction;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalFlowService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalWorkflowService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalPermissionService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalCompletionHandler;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final ApprovalInstanceRepo approvalInstanceRepo;
    private final ApprovalStepInstanceRepo approvalStepInstanceRepo;
    private final ApprovalFlowService approvalFlowService;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;

    private final ApprovalPermissionService permissionService;
    private final ApprovalCompletionHandler completionHandler;

    @Transactional
    @Override
    public void startApprovalWorkflow(ServiceType serviceName, UUID itemId, UUID organisationId, UUID contextProjectId)
            throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);

        // Check if approval already exists for this item
        if (approvalInstanceRepo.existsByServiceNameAndItemIdAndStatusIn(
                serviceName, itemId, List.of(ApprovalStatus.PENDING))) {
            throw new ItemNotFoundException("Approval workflow already active for this item");
        }

        // Get the configured flow for this service
        ApprovalFlow flow = approvalFlowService.getApprovalFlowByService(organisationId, serviceName);

        // Create approval instance
        ApprovalInstance instance = new ApprovalInstance();
        instance.setApprovalFlow(flow);
        instance.setServiceName(serviceName);
        instance.setOrganisation(organisation);
        instance.setItemId(itemId);
        instance.setContextProjectId(contextProjectId);
        instance.setOrganisation(organisation);
        instance.setStatus(ApprovalStatus.PENDING);
        instance.setCurrentStepOrder(1);
        instance.setTotalSteps(flow.getSteps().size());
        instance.setSubmittedBy(currentUser.getId());
        instance.setStartedAt(LocalDateTime.now());

        ApprovalInstance savedInstance = approvalInstanceRepo.save(instance);

        // Create step instances from flow configuration
        List<ApprovalStepInstance> stepInstances = new ArrayList<>();
        for (ApprovalStep flowStep : flow.getSteps()) {
            ApprovalStepInstance stepInstance = new ApprovalStepInstance();
            stepInstance.setApprovalInstance(savedInstance);
            stepInstance.setStepOrder(flowStep.getStepOrder());
            stepInstance.setScopeType(flowStep.getScopeType());
            stepInstance.setOrganisation(organisation);
            stepInstance.setRoleId(flowStep.getRoleId());
            stepInstance.setRequired(flowStep.isRequired());

            // The first step is PENDING, others are WAITING
            stepInstance.setStatus(flowStep.getStepOrder() == 1 ? StepStatus.PENDING : StepStatus.WAITING);

            stepInstances.add(stepInstance);
        }

        approvalStepInstanceRepo.saveAll(stepInstances);
        savedInstance.setStepInstances(stepInstances);

    }

    @Transactional
    @Override
    public ApprovalInstance processApprovalAction(UUID instanceId, ApprovalAction action, String comments)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ApprovalInstance instance = approvalInstanceRepo.findById(instanceId)
                .orElseThrow(() -> new ItemNotFoundException("Approval instance not found"));

        if (instance.getStatus() != ApprovalStatus.PENDING) {
            throw new AccessDeniedException("Approval workflow is not active");
        }

        // Get the current step
        ApprovalStepInstance currentStep = approvalStepInstanceRepo
                .findByApprovalInstanceAndStepOrder(instance, instance.getCurrentStepOrder())
                .orElseThrow(() -> new ItemNotFoundException("Current step not found"));

        if (currentStep.getStatus() != StepStatus.PENDING) {
            throw new AccessDeniedException("Step is not pending approval");
        }

        // Check permissions
        if (!permissionService.canUserApprove(currentUser, currentStep)) {
            throw new AccessDeniedException("You do not have the required role to approve this step");
        }

        // Process the action
        if (action == ApprovalAction.APPROVE) {
            //Add approval to history
            currentStep.addApprovalToHistory(
                    currentUser.getId(),
                    currentUser.getUserName(),
                    comments,
                    currentStep.getNextRevisionNumber()
            );

            //Resolve any active rejections
            currentStep.resolveActiveRejections(
                    currentUser.getUserName(),
                    "Resolved by approval: " + comments
            );

            //Set current step state
            currentStep.setApprovedBy(currentUser.getId());
            currentStep.setApprovedAt(LocalDateTime.now());
            currentStep.setComments(comments);
            currentStep.setAction(ApprovalAction.APPROVE);
            currentStep.setStatus(StepStatus.APPROVED);

            //Check if this is the last step
            if (instance.getCurrentStepOrder() >= instance.getTotalSteps()) {
                // Workflow completed
                instance.setStatus(ApprovalStatus.APPROVED);
                instance.setCompletedAt(LocalDateTime.now());
            } else {
                // Move to the next step
                instance.setCurrentStepOrder(instance.getCurrentStepOrder() + 1);

                // Set the next step to PENDING
                ApprovalStepInstance nextStep = approvalStepInstanceRepo
                        .findByApprovalInstanceAndStepOrder(instance, instance.getCurrentStepOrder())
                        .orElseThrow(() -> new ItemNotFoundException("Next step not found"));
                nextStep.setStatus(StepStatus.PENDING);
                approvalStepInstanceRepo.save(nextStep);
            }

        } else { // REJECT
            //Adds rejection to history
            currentStep.addRejectionToHistory(
                    currentUser.getUserName(),
                    comments,
                    currentStep.getNextRevisionNumber()
            );

            //Set the current step state
            currentStep.setApprovedBy(currentUser.getId());
            currentStep.setApprovedAt(LocalDateTime.now());
            currentStep.setComments(comments);
            currentStep.setAction(ApprovalAction.REJECT);
            currentStep.setStatus(StepStatus.REJECTED);

            if (instance.getCurrentStepOrder() == 1) {
                // First step rejection - terminate workflow
                instance.setStatus(ApprovalStatus.REJECTED);
                instance.setCompletedAt(LocalDateTime.now());
            } else {
                // Go back to a previous step
                instance.setCurrentStepOrder(instance.getCurrentStepOrder() - 1);

                // Set the previous step to PENDING and clear its current state
                ApprovalStepInstance previousStep = approvalStepInstanceRepo
                        .findByApprovalInstanceAndStepOrder(instance, instance.getCurrentStepOrder())
                        .orElseThrow(() -> new ItemNotFoundException("Previous step not found"));

                //Clear the current state (but history remains intact!)
                previousStep.setStatus(StepStatus.PENDING);
                previousStep.setApprovedBy(null);
                previousStep.setApprovedAt(null);
                previousStep.setComments(null);
                previousStep.setAction(null);

                approvalStepInstanceRepo.save(previousStep);
            }
        }

        approvalStepInstanceRepo.save(currentStep);
        ApprovalInstance updatedInstance = approvalInstanceRepo.save(instance);

        // Handle completion if workflow finished
        if (updatedInstance.getStatus() == ApprovalStatus.APPROVED ||
                updatedInstance.getStatus() == ApprovalStatus.REJECTED) {
            completionHandler.handleApprovalCompletion(updatedInstance);
        }

        return updatedInstance;
    }

    @Override
    public ApprovalInstance getApprovalInstanceByItem(ServiceType serviceName, UUID itemId)
            throws ItemNotFoundException {

        return approvalInstanceRepo.findByServiceNameAndItemId(serviceName, itemId)
                .orElseThrow(() -> new ItemNotFoundException("Approval instance not found for this item"));
    }

    @Override
    public List<ApprovalInstance> getMySubmittedApprovals() throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();
        return approvalInstanceRepo.findBySubmittedBy(currentUser.getId());
    }

    @Override
    public boolean canUserApproveStep(UUID instanceId, int stepOrder) throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();

        ApprovalInstance instance = approvalInstanceRepo.findById(instanceId)
                .orElseThrow(() -> new ItemNotFoundException("Approval instance not found"));

        ApprovalStepInstance step = approvalStepInstanceRepo
                .findByApprovalInstanceAndStepOrder(instance, stepOrder)
                .orElseThrow(() -> new ItemNotFoundException("Step not found"));

        // 🚀 NEW: Use permission service
        return permissionService.canUserApprove(currentUser, step);
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userName = userDetails.getUsername();

        return accountRepo.findByUserName(userName)
                .orElseThrow(() -> new ItemNotFoundException("User not found"));
    }

    private OrganisationEntity getOrganisation(UUID organisationId) throws ItemNotFoundException {
        return organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
    }
}