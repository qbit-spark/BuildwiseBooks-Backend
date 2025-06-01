package com.qbitspark.buildwisebackend.projectmngService.service.Impl;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeauthentication.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmngService.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmngService.enums.ProjectStatus;
import com.qbitspark.buildwisebackend.projectmngService.payloads.*;
import com.qbitspark.buildwisebackend.projectmngService.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmngService.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepo projectRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;

    // Creates a new project in the specified organisation with the given details and team members
    @Override
    public ProjectResponse createProject(ProjectCreateRequest request, UUID creatorMemberId, UUID organisationId) throws ItemNotFoundException {
        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember creator = validateMemberPermissions(creatorMemberId, organisationId,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        if (!creator.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Creator must be the authenticated user");
        }

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        if (projectRepo.existsByNameAndOrganisation(request.getName(), organisation)) {
            throw new ItemNotFoundException("Project with name " + request.getName() + " already exists in this organisation");
        }

        ProjectEntity project = new ProjectEntity();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setBudget(request.getBudget());
        project.setOrganisation(organisation);
        organisation.addProject(project);
        project.setCreatedBy(creator);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        if (request.getTeamMemberIds() != null && !request.getTeamMemberIds().isEmpty()) {
            Set<OrganisationMember> teamMembers = validateAndGetTeamMembers(request.getTeamMemberIds(), organisationId);
            for (OrganisationMember member : teamMembers) {
                project.addTeamMember(member);
            }
        }

        ProjectEntity savedProject = projectRepo.save(project);

        log.info("Project '{}' created successfully with ID: {} in organisation: {}",
                savedProject.getName(), savedProject.getProjectId(), organisationId);

        return mapToProjectResponse(savedProject);
    }

    // Retrieves a project by its ID, ensuring the requester has access to the project's organisation
    @Override
    public ProjectResponse getProjectById(UUID projectId, UUID requesterId) throws ItemNotFoundException {
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember requesterMember = validateMemberAccess(requesterId, project.getOrganisation().getOrganisationId());

        if (!requesterMember.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Requester must be the authenticated user");
        }

        return mapToProjectResponse(project);
    }

    // Updates an existing project's details, such as name, description, budget, or team members
    @Override
    public ProjectResponse updateProject(UUID projectId, UUID organisationId, ProjectUpdateRequest request, UUID updaterMemberId) throws ItemNotFoundException {
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        // Validate organisation ID matches project's organisation
        if (!project.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Project does not belong to the specified organisation");
        }

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember updater = validateMemberPermissions(updaterMemberId, organisationId,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        if (!updater.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Updater must be the authenticated user");
        }

        if (!project.getName().equals(request.getName()) &&
                projectRepo.existsByNameAndOrganisation(request.getName(), project.getOrganisation())) {
            throw new ItemNotFoundException("Project with name " + request.getName() + " already exists in this organisation");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            project.setName(request.getName().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            project.setDescription(request.getDescription().trim());
        }
        if (request.getBudget() != null) {
            project.setBudget(request.getBudget());
        }

        if (request.getTeamMemberIds() != null) {
            updateProjectTeamMembers(project, request.getTeamMemberIds());
        }

        project.setUpdatedAt(LocalDateTime.now());
        ProjectEntity updatedProject = projectRepo.save(project);

        log.info("Project '{}' updated successfully by member: {}", project.getName(), updaterMemberId);

        return mapToProjectResponse(updatedProject);
    }

    // Soft delete: marks project as cancelled instead of hard deletion
    @Override
    public String deleteProject(UUID projectId, UUID deleterMemberId) throws ItemNotFoundException {
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember deleter = validateMemberPermissions(deleterMemberId, project.getOrganisation().getOrganisationId(),
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        if (!deleter.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Deleter must be the authenticated user");
        }

        project.setStatus(ProjectStatus.CANCELLED);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepo.save(project);

        log.info("Project '{}' marked as cancelled by member: {}", project.getName(), deleterMemberId);

        return "Project cancelled successfully";
    }

    // Retrieves a paginated list of projects for an organisation, sorted by the specified field and direction
    @Override
    public Page<ProjectListResponse> getOrganisationProjects(
            UUID organisationId, UUID requesterId, int page, int size, String sortBy, String sortDirection) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember requesterMember = validateMemberAccess(requesterId, organisationId);

        if (!requesterMember.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Requester must be the authenticated user");
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProjectEntity> projects = projectRepo.findByOrganisationOrganisationIdAndStatusNot(
                organisationId, ProjectStatus.CANCELLED, pageable);
        return projects.map(this::mapToProjectListResponse);
    }
// Get all projects available
    @Override
    public Page<ProjectListResponse> getAllProjects(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProjectEntity> projects = projectRepo.findByStatusNot(ProjectStatus.CANCELLED, pageable);
        return projects.map(this::mapToProjectListResponse);
    }

    // Searches for projects in an organisation based on status, with pagination and sorting
    @Override
    public Page<ProjectListResponse> searchProjects(ProjectSearchRequest searchRequest, UUID requesterId) throws ItemNotFoundException {
        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember requesterMember = validateMemberAccess(requesterId, searchRequest.getOrganisationId());

        if (!requesterMember.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Requester must be the authenticated user");
        }

        Sort sort = Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), searchRequest.getSortBy());
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

        Page<ProjectEntity> projects;
        if (searchRequest.getStatus() != null && !searchRequest.getStatus().isEmpty()) {
            ProjectStatus status = ProjectStatus.valueOf(searchRequest.getStatus().toUpperCase());
            projects = projectRepo.findByOrganisationOrganisationIdAndStatus(
                    searchRequest.getOrganisationId(), status, pageable);
        } else {
            projects = projectRepo.findByOrganisationOrganisationIdAndStatusNot(
                    searchRequest.getOrganisationId(), ProjectStatus.CANCELLED, pageable);
        }

        return projects.map(this::mapToProjectListResponse);
    }

    // Updates the team members assigned to a project
    @Override
    public ProjectResponse updateProjectTeam(UUID projectId, ProjectTeamUpdateRequest request, UUID updaterMemberId) throws ItemNotFoundException {
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember updater = validateMemberPermissions(updaterMemberId, project.getOrganisation().getOrganisationId(),
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        if (!updater.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Updater must be the authenticated user");
        }

        updateProjectTeamMembers(project, request.getTeamMemberIds());
        project.setUpdatedAt(LocalDateTime.now());
        ProjectEntity updatedProject = projectRepo.save(project);

        log.info("Project '{}' team updated successfully", project.getName());

        return mapToProjectResponse(updatedProject);
    }

    // Retrieves a paginated list of projects that a specific member is part of
    @Override
    public Page<ProjectListResponse> getMemberProjects(UUID memberId, int page, int size) throws ItemNotFoundException {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProjectEntity> projects = projectRepo.findByTeamMembersMemberIdAndStatusNot(
                memberId, ProjectStatus.CANCELLED, pageable);
        return projects.map(this::mapToProjectListResponse);
    }

    // Generates statistics for all projects in an organisation, including counts and budget information
    @Override
    public ProjectStatisticsResponse getProjectStatistics(UUID organisationId, UUID requesterId) throws ItemNotFoundException {
        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember requesterMember = validateMemberAccess(requesterId, organisationId);

        if (!requesterMember.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Requester must be the authenticated user");
        }

        long totalProjects = projectRepo.countByOrganisationOrganisationId(organisationId);
        long activeProjects = projectRepo.countByOrganisationOrganisationIdAndStatus(organisationId, ProjectStatus.ACTIVE);
        long completedProjects = projectRepo.countByOrganisationOrganisationIdAndStatus(organisationId, ProjectStatus.COMPLETED);
        long pausedProjects = projectRepo.countByOrganisationOrganisationIdAndStatus(organisationId, ProjectStatus.PAUSED);
        long cancelledProjects = projectRepo.countByOrganisationOrganisationIdAndStatus(organisationId, ProjectStatus.CANCELLED);

        List<ProjectEntity> allProjects = projectRepo.findByOrganisationOrganisationId(organisationId);

        BigDecimal totalBudget = allProjects.stream()
                .map(ProjectEntity::getBudget)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBudget = totalProjects > 0 ?
                totalBudget.divide(BigDecimal.valueOf(totalProjects), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        int totalTeamMembers = allProjects.stream()
                .mapToInt(ProjectEntity::getTeamMembersCount)
                .sum();

        double averageTeamSize = totalProjects > 0 ? (double) totalTeamMembers / totalProjects : 0.0;

        return new ProjectStatisticsResponse(
                totalProjects, activeProjects, completedProjects, pausedProjects, cancelledProjects,
                totalBudget, averageBudget, totalTeamMembers, averageTeamSize
        );
    }

    // Removes a specific team member from a project
    @Override
    public ProjectResponse removeTeamMember(UUID projectId, UUID memberToRemoveId, UUID removerMemberId) throws ItemNotFoundException {
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationMember remover = validateMemberPermissions(removerMemberId, project.getOrganisation().getOrganisationId(),
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        if (!remover.getAccount().getAccountId().equals(authenticatedAccount.getAccountId())) {
            throw new ItemNotFoundException("Remover must be the authenticated user");
        }

        OrganisationMember memberToRemove = project.getTeamMembers().stream()
                .filter(member -> member.getMemberId().equals(memberToRemoveId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Member not found in project team"));

        project.removeTeamMember(memberToRemove);
        project.setUpdatedAt(LocalDateTime.now());
        ProjectEntity updatedProject = projectRepo.save(project);

        log.info("Member {} removed from project '{}'", memberToRemoveId, project.getName());

        return mapToProjectResponse(updatedProject);
    }

    // Helper method to update project team members efficiently
    private void updateProjectTeamMembers(ProjectEntity project, Set<UUID> newTeamMemberIds) throws ItemNotFoundException {
        Set<UUID> currentMemberIds = project.getTeamMembers().stream()
                .map(OrganisationMember::getMemberId)
                .collect(Collectors.toSet());

        Set<UUID> membersToRemove = new HashSet<>(currentMemberIds);
        membersToRemove.removeAll(newTeamMemberIds);

        for (UUID memberIdToRemove : membersToRemove) {
            OrganisationMember memberToRemove = project.getTeamMembers().stream()
                    .filter(member -> member.getMemberId().equals(memberIdToRemove))
                    .findFirst()
                    .orElse(null);
            if (memberToRemove != null) {
                project.removeTeamMember(memberToRemove);
            }
        }

        Set<UUID> membersToAdd = new HashSet<>(newTeamMemberIds);
        membersToAdd.removeAll(currentMemberIds);

        if (!membersToAdd.isEmpty()) {
            Set<OrganisationMember> newMembers = validateAndGetTeamMembers(
                    membersToAdd, project.getOrganisation().getOrganisationId());
            for (OrganisationMember member : newMembers) {
                project.addTeamMember(member);
            }
        }
    }

    // Retrieves the authenticated account from the security context
    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractAccount(authentication);
    }

    // Extracts the account entity from the authentication object
    private AccountEntity extractAccount(Authentication authentication) throws ItemNotFoundException {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User given username does not exist"));
        }
        throw new ItemNotFoundException("User is not authenticated");
    }

    // Validates that a member has the required permissions (role) in an organisation
    private OrganisationMember validateMemberPermissions(UUID memberId, UUID organisationId, List<MemberRole> allowedRoles) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByMemberIdAndOrganisationOrganisationId(memberId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new ItemNotFoundException("Member has insufficient permissions");
        }

        return member;
    }

    // Validates that a member has access to an organisation (active membership) and returns the member
    private OrganisationMember validateMemberAccess(UUID memberId, UUID organisationId) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByMemberIdAndOrganisationOrganisationId(memberId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    // Validates and retrieves a set of active organisation members based on their IDs
    private Set<OrganisationMember> validateAndGetTeamMembers(Set<UUID> memberIds, UUID organisationId) throws ItemNotFoundException {
        List<OrganisationMember> members = organisationMemberRepo.findByMemberIdInAndOrganisationOrganisationIdAndStatus(memberIds, organisationId, MemberStatus.ACTIVE);

        if (members.size() != memberIds.size()) {
            Set<UUID> foundMemberIds = members.stream()
                    .map(OrganisationMember::getMemberId)
                    .collect(Collectors.toSet());
            Set<UUID> missingMemberIds = new HashSet<>(memberIds);
            missingMemberIds.removeAll(foundMemberIds);

            throw new ItemNotFoundException("The following team members are not valid or active in the organisation: " + missingMemberIds);
        }

        return new HashSet<>(members);
    }

    // Maps a ProjectEntity to a ProjectResponse for detailed project information
    private ProjectResponse mapToProjectResponse(ProjectEntity project) {
        ProjectResponse response = new ProjectResponse();
        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setBudget(project.getBudget());
        response.setOrganisationName(project.getOrganisation().getOrganisationName());
        response.setOrganisationId(project.getOrganisation().getOrganisationId());
        response.setStatus(project.getStatus().name());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        if (project.getCreatedBy() != null) {
            response.setCreatedBy(mapToTeamMemberResponse(project.getCreatedBy()));
        }

        Set<TeamMemberResponse> teamMemberResponses = project.getTeamMembers().stream()
                .map(this::mapToTeamMemberResponse)
                .collect(Collectors.toSet());
        response.setTeamMembers(teamMemberResponses);

        return response;
    }

    // Maps a ProjectEntity to a ProjectListResponse for summarized project information
    private ProjectListResponse mapToProjectListResponse(ProjectEntity project) {
        ProjectListResponse response = new ProjectListResponse();
        response.setProjectId(project.getProjectId());
        response.setProjectName(project.getName());
        response.setProjectDescription(project.getDescription());
        response.setBudget(project.getBudget());
        response.setStatus(project.getStatus().name());
        response.setOrganisationName(project.getOrganisation().getOrganisationName());
        response.setTeamMembersCount(project.getTeamMembersCount());
        response.setCreatedAt(project.getCreatedAt());
        return response;
    }

    // Maps an OrganisationMember to a TeamMemberResponse for team member details
    private TeamMemberResponse mapToTeamMemberResponse(OrganisationMember member) {
        TeamMemberResponse response = new TeamMemberResponse();
        response.setMemberId(member.getMemberId());
        response.setMemberName(member.getAccount().getUserName());
        response.setEmail(member.getAccount().getEmail());
        response.setRole(member.getRole().name());
        response.setStatus(member.getStatus().name());
        response.setJoinedAt(member.getJoinedAt());
        return response;
    }
}