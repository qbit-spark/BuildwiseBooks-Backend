package com.qbitspark.buildwisebackend.projectmng_service.service.Impl;

import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamMemberService;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamRoleService;
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
    private final ProjectTeamRoleRepo projectTeamRoleRepo;
    private final ProjectTeamRoleService projectTeamRoleService;
    private final AccountRepo accountRepo;
    private final AsyncEmailService asyncEmailService;
    private final OrganisationRepo organisationRepo;
    private final PermissionCheckerService permissionChecker;

    @Transactional
    @Override
    public List<ProjectTeamMemberResponse> addTeamMembers(UUID organisationId, UUID projectId, Set<BulkAddTeamMemberRequest> requests) throws Exception {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        ProjectEntity project = projectRepo.findByOrganisationAndProjectId(organisation, projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        return addTeamMembersInternal(project, requests, true);
    }

    @Transactional
    @Override
    public void addCreatorAndOwnerAsTeamMembers(ProjectEntity project, OrganisationMember creator, OrganisationMember ownerOfOrganisation) throws Exception {

        Set<BulkAddTeamMemberRequest> requests = new HashSet<>();


        ProjectTeamRoleEntity projectManagerRole = projectTeamRoleService.getProjectTeamRoleByName(
                project.getOrganisation(), "PROJECT_MANAGER");


        BulkAddTeamMemberRequest creatorRequest = new BulkAddTeamMemberRequest();
        creatorRequest.setMemberIds(Set.of(creator.getMemberId()));
        creatorRequest.setRoleId(projectManagerRole.getRoleId());
        requests.add(creatorRequest);

        // Add owner if different
        if (!creator.getMemberId().equals(ownerOfOrganisation.getMemberId())) {
            BulkAddTeamMemberRequest ownerRequest = new BulkAddTeamMemberRequest();
            ownerRequest.setMemberIds(Set.of(ownerOfOrganisation.getMemberId()));
            ownerRequest.setRoleId(projectManagerRole.getRoleId());
            requests.add(ownerRequest);
        }

        addTeamMembersInternal(project, requests, false);
    }

    @Override
    public ProjectTeamRemovalResponse removeTeamMembers(UUID organisationId, UUID projectId, Set<UUID> memberIds)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        List<ProjectTeamMemberEntity> allProjectMembers = projectTeamMemberRepo.findByProjectProjectId(projectId);

        List<ProjectTeamMemberEntity> membersToRemove = new ArrayList<>();
        List<UUID> skippedMembers = new ArrayList<>();
        List<UUID> protectedOwners = new ArrayList<>();

        for (UUID memberId : memberIds) {
            Optional<ProjectTeamMemberEntity> teamMemberOpt = allProjectMembers.stream()
                    .filter(tm -> tm.getOrganisationMember().getMemberId().equals(memberId))
                    .findFirst();

            if (teamMemberOpt.isPresent()) {
                ProjectTeamMemberEntity teamMember = teamMemberOpt.get();
                OrganisationMember orgMember = teamMember.getOrganisationMember();

                // Check if member has an OWNER role
                boolean isOrgOwner = "OWNER".equalsIgnoreCase(orgMember.getMemberRole().getRoleName());

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
        for (ProjectTeamMemberEntity memberToRemove : membersToRemove) {
            try {
                removedMembers.add(mapToResponse(memberToRemove));
                projectTeamMemberRepo.delete(memberToRemove);
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
    public Page<ProjectTeamMemberResponse> getProjectTeamMembers(UUID organisationId, UUID projectId, Pageable pageable) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        ProjectEntity project = projectRepo.findByOrganisationAndProjectId(organisation, projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        Page<ProjectTeamMemberEntity> teamMembersPage = projectTeamMemberRepo.findByProject(project, pageable);

        return teamMembersPage.map(this::mapToResponse);
    }

    @Override
    public ProjectTeamMemberResponse updateTeamMemberRole(UUID organisationId, UUID projectId, UUID memberId, UpdateTeamMemberRoleRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        ProjectEntity project = projectRepo.findByOrganisationAndProjectId(organisation, projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        ProjectTeamMemberEntity teamMember = project.getTeamMembers().stream()
                .filter(tm -> tm.getOrganisationMember().getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Member is not part of this project"));

        // Check if the member being updated is an organisation owner
        OrganisationMember orgMember = teamMember.getOrganisationMember();
        if (orgMember.getMemberRole().getRoleName().equals("OWNER")) {
            throw new ItemNotFoundException("Organisation owners' project roles cannot be modified");
        }

        // Validate the new role exists and belongs to the organisation
        ProjectTeamRoleEntity newRole = projectTeamRoleRepo.findById(request.getNewRoleId())
                .orElseThrow(() -> new ItemNotFoundException("Project team role not found"));

        if (!newRole.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Role does not belong to this organisation");
        }

        if (!newRole.getIsActive()) {
            throw new ItemNotFoundException("Cannot assign inactive role");
        }

        teamMember.setProjectTeamRole(newRole);
        ProjectTeamMemberEntity savedMember = projectTeamMemberRepo.save(teamMember);

        return mapToResponse(savedMember);
    }

    @Override
    public List<AvailableTeamMemberResponse> getAvailableTeamMembers(UUID organisationId, UUID projectId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        ProjectEntity project = projectRepo.findByOrganisationAndProjectId(organisation, projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        List<OrganisationMember> allOrgMembers = organisationMemberRepo
                .findByOrganisationAndStatus(project.getOrganisation(), MemberStatus.ACTIVE);

        Set<UUID> currentProjectMemberIds = projectTeamMemberRepo
                .findByProjectProjectId(projectId)
                .stream()
                .map(tm -> tm.getOrganisationMember().getMemberId())
                .collect(Collectors.toSet());

        // Filter out members who are already in the project
        List<OrganisationMember> availableMembers = allOrgMembers.stream()
                .filter(memberInProject -> !currentProjectMemberIds.contains(memberInProject.getMemberId()))
                .toList();

        return availableMembers.stream()
                .map(this::mapToAvailableResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isTeamMember(UUID organisationId, UUID projectId, UUID memberId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "PROJECTS", "viewTeamMembers");

        ProjectEntity project = projectRepo.findByOrganisationAndProjectId(organisation, projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        return project.getTeamMembers().stream()
                .anyMatch(tm -> tm.getOrganisationMember().getMemberId().equals(memberId));
    }

    // Private helper methods

    private OrganisationMember validateOrganisationMember(UUID memberId, ProjectEntity project) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findById(memberId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation member not found"));

        if (!member.getOrganisation().getOrganisationId().equals(project.getOrganisation().getOrganisationId())) {
            throw new ItemNotFoundException("Member does not belong to this project's organisation");
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    private ProjectTeamMemberResponse mapToResponse(ProjectTeamMemberEntity teamMember) {
        ProjectTeamMemberResponse response = new ProjectTeamMemberResponse();
        response.setMemberId(teamMember.getOrganisationMember().getMemberId());
        response.setMemberName(teamMember.getOrganisationMember().getAccount().getUserName());
        response.setMemberEmail(teamMember.getOrganisationMember().getAccount().getEmail());
        response.setRoleId(teamMember.getProjectTeamRole().getRoleId());
        response.setRoleName(teamMember.getProjectTeamRole().getRoleName());
        response.setRoleDescription(teamMember.getProjectTeamRole().getDescription());
        response.setOrganisationRole(teamMember.getOrganisationMember().getMemberRole().getRoleName());
        response.setStatus(teamMember.getOrganisationMember().getStatus().name());
        response.setJoinedAt(teamMember.getJoinedAt());

        if (teamMember.getAddedBy() != null) {
            accountRepo.findById(teamMember.getAddedBy())
                    .ifPresent(adder -> response.setAddedBy(adder.getUserName()));
        }

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

    private List<ProjectTeamMemberResponse> addTeamMembersInternal(ProjectEntity project,
                                                                   Set<BulkAddTeamMemberRequest> requests, Boolean sendEmail) throws Exception {

        Set<UUID> currentMemberIds = getExistingMemberIds(project);
        List<ProjectTeamMemberEntity> newMembers = createNewTeamMembers(project, requests, currentMemberIds);

        List<ProjectTeamMemberEntity> savedMembers = projectTeamMemberRepo.saveAll(newMembers);

        if (sendEmail) {
            sendEmailNotifications(savedMembers, project);
        }

        return mapToResponseList(savedMembers);
    }




    private Set<UUID> getExistingMemberIds(ProjectEntity project) {
        return projectTeamMemberRepo.findByProject(project).stream()
                .map(tm -> tm.getOrganisationMember().getMemberId())
                .collect(Collectors.toSet());
    }

    private List<ProjectTeamMemberEntity> createNewTeamMembers(ProjectEntity project,
                                                               Set<BulkAddTeamMemberRequest> requests, Set<UUID> currentMemberIds) throws ItemNotFoundException {

        List<ProjectTeamMemberEntity> newMembers = new ArrayList<>();

        for (BulkAddTeamMemberRequest request : requests) {
            ProjectTeamRoleEntity projectRole = validateAndGetProjectRole(project, request.getRoleId());

            for (UUID memberId : request.getMemberIds()) {
                if (!currentMemberIds.contains(memberId)) {
                    createAndAddTeamMember(project, projectRole, memberId, newMembers, currentMemberIds);
                }
            }
        }

        return newMembers;
    }

    private ProjectTeamRoleEntity validateAndGetProjectRole(ProjectEntity project, UUID roleId) throws ItemNotFoundException {
        ProjectTeamRoleEntity projectRole = projectTeamRoleRepo.findById(roleId)
                .orElseThrow(() -> new ItemNotFoundException("Project team role not found"));

        if (!projectRole.getOrganisation().getOrganisationId().equals(project.getOrganisation().getOrganisationId())) {
            throw new ItemNotFoundException("Role does not belong to this organisation");
        }

        if (!projectRole.getIsActive()) {
            throw new ItemNotFoundException("Cannot assign inactive role");
        }

        return projectRole;
    }

    private void createAndAddTeamMember(ProjectEntity project, ProjectTeamRoleEntity projectRole,
                                        UUID memberId, List<ProjectTeamMemberEntity> newMembers, Set<UUID> currentMemberIds) {

        try {
            OrganisationMember orgMember = validateOrganisationMember(memberId, project);

            ProjectTeamMemberEntity teamMember = new ProjectTeamMemberEntity();
            teamMember.setProject(project);
            teamMember.setOrganisationMember(orgMember);
            teamMember.setProjectTeamRole(projectRole);
            teamMember.setAddedBy(getAuthenticatedAccount().getId());

            newMembers.add(teamMember);
            currentMemberIds.add(memberId);
        } catch (ItemNotFoundException e) {
            log.warn("Skipping invalid member {}: {}", memberId, e.getMessage());
        }
    }

    private void sendEmailNotifications(List<ProjectTeamMemberEntity> members, ProjectEntity project) {
        if (members.isEmpty()) return;

        for (ProjectTeamMemberEntity member : members) {
            AccountEntity account = member.getOrganisationMember().getAccount();
            asyncEmailService.sendProjectTeamMemberAddedEmailAsync(
                    account.getEmail(),
                    account.getUserName(),
                    project.getName(),
                    member.getProjectTeamRole().getRoleName(),
                    project.getOrganisation().getOrganisationId(),
                    project.getProjectId()
            );
        }
    }

    private List<ProjectTeamMemberResponse> mapToResponseList(List<ProjectTeamMemberEntity> members) {
        return members.stream()
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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member does not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }
}