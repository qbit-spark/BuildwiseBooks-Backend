package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStep;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.payloads.CreateApprovalFlowRequest;
import com.qbitspark.buildwisebackend.approval_service.payloads.CreateApprovalStepRequest;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalFlowRepo;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepRepo;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalFlowService;
import com.qbitspark.buildwisebackend.authentication_service.repo.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.OrgMemberRoleRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
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
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private final ApprovalFlowRepo approvalFlowRepo;
    private final ApprovalStepRepo approvalStepRepo;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final PermissionCheckerService permissionChecker;
    private final OrgMemberRoleRepo orgMemberRoleRepo;
    private final ProjectTeamRoleRepo projectTeamRoleRepo;

    @Transactional
    @Override
    public ApprovalFlow createApprovalFlow(UUID organisationId, CreateApprovalFlowRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "ORGANISATION", "manageMembers");

        // Check if flow already exists FIRST
        if (approvalFlowRepo.existsByServiceNameAndOrganisationAndIsActiveTrue(request.getServiceName(), organisation)) {
            throw new ItemNotFoundException("Approval flow already exists for service: " + request.getServiceName());
        }

        // Validate ALL roles BEFORE saving anything
        for (CreateApprovalStepRequest stepRequest : request.getSteps()) {
            validateRoleForScope(stepRequest.getRoleId(), stepRequest.getScopeType(), organisation);
        }

        // Now create and save the flow
        ApprovalFlow approvalFlow = new ApprovalFlow();
        approvalFlow.setServiceName(request.getServiceName());
        approvalFlow.setDescription(request.getDescription());
        approvalFlow.setOrganisation(organisation);
        approvalFlow.setIsActive(true);
        approvalFlow.setCreatedAt(LocalDateTime.now());
        approvalFlow.setUpdatedAt(LocalDateTime.now());

        ApprovalFlow savedFlow = approvalFlowRepo.save(approvalFlow);

        // Create steps (validation already done)
        List<ApprovalStep> steps = new ArrayList<>();
        for (CreateApprovalStepRequest stepRequest : request.getSteps()) {
            ApprovalStep step = new ApprovalStep();
            step.setApprovalFlow(savedFlow);
            step.setStepOrder(stepRequest.getStepOrder());
            step.setScopeType(stepRequest.getScopeType());
            step.setRoleId(stepRequest.getRoleId());
            step.setRequired(stepRequest.isRequired());
            steps.add(step);
        }

        approvalStepRepo.saveAll(steps);
        savedFlow.setSteps(steps);

        return savedFlow;
    }

    @Transactional
    @Override
    public ApprovalFlow updateApprovalFlow(UUID organisationId, UUID flowId, CreateApprovalFlowRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = getOrganisation(organisationId);

        ApprovalFlow existingFlow = approvalFlowRepo.findByOrganisationAndFlowId(organisation, flowId)
                .orElseThrow(() -> new ItemNotFoundException("Approval flow not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "ORGANISATION", "manageMembers");

        existingFlow.setDescription(request.getDescription());
        existingFlow.setUpdatedAt(LocalDateTime.now());

        approvalStepRepo.deleteByApprovalFlow(existingFlow);

        List<ApprovalStep> steps = new ArrayList<>();
        for (CreateApprovalStepRequest stepRequest : request.getSteps()) {

            // Validate role ID based on scope type
            validateRoleForScope(stepRequest.getRoleId(), stepRequest.getScopeType(), existingFlow.getOrganisation());

            ApprovalStep step = new ApprovalStep();
            step.setApprovalFlow(existingFlow);
            step.setStepOrder(stepRequest.getStepOrder());
            step.setScopeType(stepRequest.getScopeType());
            step.setRoleId(stepRequest.getRoleId());
            step.setRequired(stepRequest.isRequired());
            steps.add(step);
        }

        approvalStepRepo.saveAll(steps);
        existingFlow.setSteps(steps);

        return approvalFlowRepo.save(existingFlow);
    }

    @Override
    public ApprovalFlow getApprovalFlowByService(UUID organisationId, ServiceType serviceName)
            throws ItemNotFoundException {

        OrganisationEntity organisation = getOrganisation(organisationId);

        return approvalFlowRepo.findByServiceNameAndOrganisationAndIsActiveTrue(serviceName, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Approval flow not found for service: " + serviceName));
    }

    @Override
    public List<ApprovalFlow> getAllApprovalFlows(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "ORGANISATION", "viewOrganisation");

        return approvalFlowRepo.findByOrganisationAndIsActiveTrue(organisation);
    }

    @Transactional
    @Override
    public void deleteApprovalFlow(UUID organisationId, UUID flowId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = getOrganisation(organisationId);

        ApprovalFlow approvalFlow = approvalFlowRepo.findByOrganisationAndFlowId(organisation, flowId)
                .orElseThrow(() -> new ItemNotFoundException("Approval flow not found in organisation"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, approvalFlow.getOrganisation());
        permissionChecker.checkMemberPermission(member, "ORGANISATION", "manageMembers");

        approvalFlow.setIsActive(false);
        approvalFlow.setUpdatedAt(LocalDateTime.now());
        approvalFlowRepo.save(approvalFlow);
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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation)
            throws ItemNotFoundException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    private void validateRoleForScope(UUID roleId, ScopeType scopeType, OrganisationEntity organisation)
            throws ItemNotFoundException {

        switch (scopeType) {
            case ORGANIZATION -> {
                // Check if role exists in organization member roles
                OrgMemberRoleEntity orgRole = orgMemberRoleRepo.findById(roleId)
                        .orElseThrow(() -> new ItemNotFoundException("Organization role not found with ID: " + roleId));

                if (!orgRole.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
                    throw new ItemNotFoundException("Role does not belong to this organization");
                }

                if (!orgRole.getIsActive()) {
                    throw new ItemNotFoundException("Organization role is not active");
                }
            }
            case PROJECT -> {
                // Check if role exists in project team roles
                ProjectTeamRoleEntity projectRole = projectTeamRoleRepo.findById(roleId)
                        .orElseThrow(() -> new ItemNotFoundException("Project role not found with ID: " + roleId));

                if (!projectRole.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
                    throw new ItemNotFoundException("Project role does not belong to this organization");
                }

                if (!projectRole.getIsActive()) {
                    throw new ItemNotFoundException("Project role is not active");
                }
            }
            default -> throw new ItemNotFoundException("Invalid scope type: " + scopeType);
        }
    }
}