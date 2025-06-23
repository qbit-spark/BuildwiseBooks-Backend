package com.qbitspark.buildwisebackend.projectmng_service.service.Impl;

import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.repo.ClientsRepo;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.ProjectStatus;
import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectCodeSequenceService;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectService;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamMemberService;
import jakarta.validation.constraints.NotNull;
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
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepo projectRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final ProjectTeamMemberService projectTeamMemberService;
    private final ClientsRepo clientsRepo;
    private final ProjectCodeSequenceService projectCodeSequenceService;

    @Transactional
    @Override
    public ProjectResponse createProject(ProjectCreateRequest request, UUID organisationId) throws Exception {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        OrganisationMember creator = validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        ClientEntity client = clientsRepo.findClientEntitiesByClientIdAndOrganisation(request.getClientId(), organisation)
                .orElseThrow(() -> new ItemNotFoundException("Client does not exist in this organisation"));


        if (projectRepo.existsByNameAndOrganisation(request.getName(), organisation)) {
            throw new ItemNotFoundException("Project with name " + request.getName() + " already exists in this organisation");
        }

        String projectCode = projectCodeSequenceService.generateProjectCode(organisation.getOrganisationId());

        ProjectEntity project = new ProjectEntity();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setBudget(request.getBudget());
        project.setOrganisation(organisation);
        organisation.addProject(project);
        project.setCreatedBy(creator);
        project.setProjectCode(projectCode);
        project.setClient(client);
        project.setContractNumber(request.getContractNumber());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        ProjectEntity savedProject = projectRepo.save(project);


        // Create a default team member for the creator AND Get the actual organization owner
        OrganisationMember organisationOwner = organisationMemberRepo
                .findByOrganisationAndRole(organisation, MemberRole.OWNER)
                .orElseThrow(() -> new ItemNotFoundException("Organisation owner member not found"));

        // Pass creator and actual owner
        projectTeamMemberService.addCreatorAndOwnerAsTeamMembers(
                savedProject,
                creator,
                organisationOwner
        );

        return mapToProjectResponse(savedProject);
    }

    @Override
    public Page<ProjectResponse> getAllProjectsFromOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateOrganisationMemberAccess(authenticatedAccount, organisation);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ProjectEntity> projectPage = projectRepo.findAllByOrganisation(organisation, pageable);

         return projectPage.map(this::mapToProjectResponse);
    }

    @Override
    public ProjectResponse getProjectById(UUID projectId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        OrganisationMember projectTeamMember = validateOrganisationMemberAccess(authenticatedAccount, project.getOrganisation());

        //Make sure the authenticated user is a team member in the project in organisation
        if (!validateOrganisationMemberIsTeamMemberInProject(projectTeamMember, project)) {
            throw new AccessDeniedException("You do not have permission to access this project");
        }

        return mapToProjectResponse(project);
    }

    @Override
    public ProjectResponse updateProject(UUID projectId, ProjectUpdateRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        OrganisationMember updater = validateMemberPermissions(authenticatedAccount, project.getOrganisation(),
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        if (!validateOrganisationMemberIsTeamMemberInProject(updater, project)) {
            throw new AccessDeniedException("Your not a team member in this project");
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
        if (request.getContractNumber() != null) {
            project.setContractNumber(request.getContractNumber());
        }

        project.setUpdatedAt(LocalDateTime.now());
        ProjectEntity updatedProject = projectRepo.save(project);

        return mapToProjectResponse(updatedProject);
    }

    @Override
    public Boolean deleteProject(UUID projectId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        // Validate user is OWNER/ADMIN of THIS PROJECT'S organisation
        OrganisationMember deleter = validateMemberPermissions(
                authenticatedAccount,
                project.getOrganisation(),
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN)
        );

        project.setStatus(ProjectStatus.DELETED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setDeletedByMemberId(deleter.getMemberId());
        ProjectEntity deletedProject = projectRepo.save(project);

        if (!deletedProject.getStatus().equals(ProjectStatus.DELETED)) {
            return false;
        }

        return true;
    }

    @Override
    public Page<ProjectResponse> getAllProjectsAmBelongingToOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Get all projects where the member is a team member (excluding deleted projects)
        Page<ProjectEntity> projectPage = projectRepo.findByTeamMembersOrganisationMemberAndStatusNot(
                member, ProjectStatus.DELETED, pageable);

        return projectPage.map(this::mapToProjectResponse);
    }


    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractAccount(authentication);
    }

    private AccountEntity extractAccount(Authentication authentication) throws ItemNotFoundException {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User given username does not exist"));
        }
        throw new ItemNotFoundException("User is not authenticated");
    }

    private OrganisationMember validateMemberPermissions(AccountEntity account, OrganisationEntity organisation, List<MemberRole> allowedRoles) throws ItemNotFoundException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new ItemNotFoundException("Member has insufficient permissions");
        }

        return member;
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    private boolean validateOrganisationMemberIsTeamMemberInProject(OrganisationMember organisationMember, ProjectEntity project) {
        return project.isTeamMember(organisationMember);
    }

    private ProjectResponse mapToProjectResponse(ProjectEntity project) {

        return getProjectResponse(project);
    }

    public static ProjectResponse getProjectResponse(ProjectEntity project) {
        ProjectResponse response = new ProjectResponse();
        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setBudget(project.getBudget());
        response.setOrganisationName(project.getOrganisation().getOrganisationName());
        response.setOrganisationId(project.getOrganisation().getOrganisationId());
        response.setStatus(project.getStatus().name());
        response.setProjectCode(project.getProjectCode());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        response.setClientId(project.getClient().getClientId());
        response.setClientName(project.getClient().getName());
        response.setContractNumber(project.getContractNumber());
        response.setCreatedBy(project.getCreatedBy().getAccount().getUserName());

        return response;
    }
}