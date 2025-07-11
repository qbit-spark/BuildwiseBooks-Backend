package com.qbitspark.buildwisebackend.projectmng_service.service.Impl;

import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamMemberService;
import com.qbitspark.buildwisebackend.projectmng_service.utils.AsyncEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor

@Slf4j
public class ProjectTeamMembersServiceImpl implements ProjectTeamMemberService {

    private final ProjectRepo projectRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final AccountRepo accountRepo;
    private final AsyncEmailService asyncEmailService;

    @Transactional
    @Override
    public List<ProjectTeamMemberResponse> addTeamMembers(UUID projectId, Set<BulkAddTeamMemberRequest> requests) throws Exception {

        validateRequesterPermissions(projectId);

        return addTeamMembersInternal(projectId, requests, true);

    }

    @Transactional
    @Override
    public void addCreatorAndOwnerAsTeamMembers(ProjectEntity project, OrganisationMember creator, OrganisationMember ownerOfOrganisation) throws Exception {

        Set<BulkAddTeamMemberRequest> requests = new HashSet<>();

        // Add creator
        BulkAddTeamMemberRequest creatorRequest = new BulkAddTeamMemberRequest();
        creatorRequest.setMemberIds(Set.of(creator.getMemberId()));
        creatorRequest.setRole(TeamMemberRole.PROJECT_MANAGER);
        requests.add(creatorRequest);

        // Add an owner if different
        if (!creator.getMemberId().equals(ownerOfOrganisation.getMemberId())) {
            BulkAddTeamMemberRequest ownerRequest = new BulkAddTeamMemberRequest();
            ownerRequest.setMemberIds(Set.of(ownerOfOrganisation.getMemberId()));
            ownerRequest.setRole(TeamMemberRole.PROJECT_MANAGER);
            requests.add(ownerRequest);
        }

        addTeamMembersInternal(project.getProjectId(), requests, true);
    }

    @Override
    public ProjectTeamRemovalResponse  removeTeamMembers(UUID projectId, Set<UUID> memberIds) throws ItemNotFoundException {

        validateRequesterPermissions(projectId);

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        List<ProjectTeamMemberEntity> allProjectMembers = projectTeamMemberRepo.findByProjectProjectId(projectId);

        // Get the organisation owner
        OrganisationMember orgOwner = organisationMemberRepo
                .findByOrganisationAndRole(project.getOrganisation(), MemberRole.OWNER)
                .orElse(null);

        List<ProjectTeamMemberEntity> membersToRemove = new ArrayList<>();
        List<UUID> skippedMembers = new ArrayList<>();
        List<UUID> protectedOwners = new ArrayList<>();

        for (UUID memberId : memberIds) {
            Optional<ProjectTeamMemberEntity> teamMemberOpt = allProjectMembers.stream()
                    .filter(tm -> tm.getOrganisationMember().getMemberId().equals(memberId))
                    .findFirst();

            if (teamMemberOpt.isPresent()) {
                ProjectTeamMemberEntity teamMember = teamMemberOpt.get();

                boolean isOrgOwner = orgOwner != null &&
                        orgOwner.getMemberId().equals(teamMember.getOrganisationMember().getMemberId());

                if (isOrgOwner) {
                    protectedOwners.add(memberId);
                } else {
                    membersToRemove.add(teamMember);
                }
            } else {
                skippedMembers.add(memberId);
            }
        }

        // Remove members
        List<ProjectTeamMemberResponse> removedMembers = new ArrayList<>();
        for (ProjectTeamMemberEntity member : membersToRemove) {
            try {
                removedMembers.add(mapToResponse(member));
                projectTeamMemberRepo.delete(member);
            } catch (Exception e) {
                log.error("Failed to remove member: {}", e.getMessage());
            }
        }

        // Create detailed response
        ProjectTeamRemovalResponse response = new ProjectTeamRemovalResponse();
        response.setRemovedMembers(removedMembers);
        response.setSkippedMemberIds(skippedMembers);
        response.setProtectedOwnerIds(protectedOwners);
        response.setTotalRequested(memberIds.size());
        response.setTotalRemoved(removedMembers.size());
        response.setTotalSkipped(skippedMembers.size());
        response.setTotalProtected(protectedOwners.size());

        if (!protectedOwners.isEmpty()) {
            response.setMessage("Some members were protected from removal (organisation owners cannot be removed from projects)");
        }

        return response;
    }


    @Override
    public Page<ProjectTeamMemberResponse> getProjectTeamMembers(UUID projectId, Pageable pageable) throws ItemNotFoundException {
        validateRequesterAccess(projectId);

        if (!projectRepo.existsById(projectId)) {
            throw new ItemNotFoundException("Project not found");
        }

        Page<ProjectTeamMemberEntity> teamMembersPage =
                projectTeamMemberRepo.findByProjectProjectId(projectId, pageable);

        return teamMembersPage.map(this::mapToResponse);
    }

    @Override
    public ProjectTeamMemberResponse updateTeamMemberRole(UUID projectId, UUID memberId, UpdateTeamMemberRoleRequest request) throws ItemNotFoundException {

        validateRequesterPermissions(projectId);

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        ProjectTeamMemberEntity teamMember = project.getTeamMembers().stream()
                .filter(tm -> tm.getOrganisationMember().getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Member is not part of this project"));

        // Check if the member being updated is an organisation owner
        OrganisationMember orgMember = teamMember.getOrganisationMember();
        if (orgMember.getRole() == MemberRole.OWNER) {
            throw new ItemNotFoundException("Organisation owners' project roles cannot be modified");
        }

        teamMember.setRole(request.getNewRole());
        ProjectTeamMemberEntity savedMember = projectTeamMemberRepo.save(teamMember);

        log.info("Updated team member {} role to {} in project {}",
                memberId, request.getNewRole(), project.getName());

        return mapToResponse(savedMember);
    }



    @Override
    public List<AvailableTeamMemberResponse> getAvailableTeamMembers(UUID projectId) throws ItemNotFoundException {
        validateRequesterAccess(projectId);

        // Get the project and validate it exists
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        // Get all active organisation members for the project's organisation
        List<OrganisationMember> allOrgMembers = organisationMemberRepo
                .findByOrganisationAndStatus(project.getOrganisation(), MemberStatus.ACTIVE);

        // Get current project team member IDs
        Set<UUID> currentProjectMemberIds = projectTeamMemberRepo
                .findByProjectProjectId(projectId)
                .stream()
                .map(tm -> tm.getOrganisationMember().getMemberId())
                .collect(Collectors.toSet());

        // Filter out members who are already in the project
        List<OrganisationMember> availableMembers = allOrgMembers.stream()
                .filter(member -> !currentProjectMemberIds.contains(member.getMemberId()))
                .toList();


        // Map to response DTOs
        return availableMembers.stream()
                .map(this::mapToAvailableResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isTeamMember(UUID projectId, UUID memberId) throws ItemNotFoundException {

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        return project.getTeamMembers().stream()
                .anyMatch(tm -> tm.getOrganisationMember().getMemberId().equals(memberId));
    }

    private OrganisationMember validateOrganisationMember(UUID memberId, ProjectEntity project) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findById(memberId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation member not found"));

        // Validate member belongs to project's organisation
        if (!member.getOrganisation().getOrganisationId().equals(project.getOrganisation().getOrganisationId())) {
            throw new ItemNotFoundException("Member does not belong to this project's organisation");
        }

        // Validate member is active
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    private void validateRequesterPermissions(UUID projectId) throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember requester = organisationMemberRepo
                .findByAccountAndOrganisation(currentUser, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("You are not a member of this project's organisation"));

        if (requester.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Your membership is not active");
        }

        // Only OWNER, ADMIN can manage team members
        if (requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.ADMIN) {
            throw new ItemNotFoundException("You don't have permission to manage this project's team");
        }
    }

    private void validateRequesterAccess(UUID projectId) throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember requester = organisationMemberRepo
                .findByAccountAndOrganisation(currentUser, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("You are not a member of this project's organisation"));

        if (requester.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Your membership is not active");
        }
    }

    private ProjectTeamMemberResponse mapToResponse(ProjectTeamMemberEntity teamMember) {
        ProjectTeamMemberResponse response = new ProjectTeamMemberResponse();
        response.setMemberId(teamMember.getOrganisationMember().getMemberId());
        response.setMemberName(teamMember.getOrganisationMember().getAccount().getUserName());
        response.setMemberEmail(teamMember.getOrganisationMember().getAccount().getEmail());
        response.setRole(teamMember.getRole());
        response.setRole(teamMember.getRole());
        response.setOrganisationRole(teamMember.getOrganisationMember().getRole().name());
        response.setStatus(teamMember.getOrganisationMember().getStatus().name());
        response.setJoinedAt(teamMember.getOrganisationMember().getJoinedAt());
        return response;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    private List<ProjectTeamMemberResponse> addTeamMembersInternal(UUID projectId, Set<BulkAddTeamMemberRequest> requests, Boolean sendEmail) throws Exception {

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        // Get existing team members
        List<ProjectTeamMemberEntity> existingMembers = projectTeamMemberRepo.findByProjectProjectId(projectId);
        Set<UUID> currentMemberIds = existingMembers.stream()
                .map(tm -> tm.getOrganisationMember().getMemberId())
                .collect(Collectors.toSet());

        List<ProjectTeamMemberEntity> newMembers = new ArrayList<>();

        for (BulkAddTeamMemberRequest request : requests) {
            for (UUID memberId : request.getMemberIds()) {

                // Skip if already exists
                if (currentMemberIds.contains(memberId)) {
                    continue;
                }

                try {
                    OrganisationMember orgMember = validateOrganisationMember(memberId, project);

                    ProjectTeamMemberEntity teamMember = new ProjectTeamMemberEntity();
                    teamMember.setProject(project);
                    teamMember.setOrganisationMember(orgMember);
                    teamMember.setRole(request.getRole());

                    newMembers.add(teamMember);
                    currentMemberIds.add(memberId);

                } catch (ItemNotFoundException e) {
                    log.warn("Skipping invalid member {}: {}", memberId, e.getMessage());
                }
            }
        }

        // Save and return
        List<ProjectTeamMemberEntity> savedMembers = projectTeamMemberRepo.saveAll(newMembers);

        //Send email notifications if required
        if (sendEmail && !savedMembers.isEmpty()) {
            for (ProjectTeamMemberEntity member : savedMembers) {
                AccountEntity account = member.getOrganisationMember().getAccount();
                asyncEmailService.sendProjectTeamMemberAddedEmailAsync(
                        account.getEmail(),
                        account.getUserName(),
                        project.getName(),
                        String.valueOf(member.getRole()),
                        project.getOrganisation().getOrganisationId(),
                        project.getProjectId()
                );
            }

        }
        return savedMembers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AvailableTeamMemberResponse mapToAvailableResponse(OrganisationMember orgMember) {
        AvailableTeamMemberResponse response = new AvailableTeamMemberResponse();
        response.setMemberId(orgMember.getMemberId());
        response.setUserName(orgMember.getAccount().getUserName());
        response.setEmail(orgMember.getAccount().getEmail());
        response.setJoinedAt(orgMember.getJoinedAt());
        return response;
    }

}