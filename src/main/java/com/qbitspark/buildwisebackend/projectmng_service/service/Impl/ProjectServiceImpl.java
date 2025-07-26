package com.qbitspark.buildwisebackend.projectmng_service.service.Impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.repo.ClientsRepo;
import com.qbitspark.buildwisebackend.drive_mng.service.OrgDriveService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.authentication_service.repo.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.ProjectStatus;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectCodeSequenceService;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectService;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamMemberService;
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
    private final OrgBudgetRepo orgBudgetRepo;
    private final OrgDriveService orgDriveService;
    private final PermissionCheckerService permissionChecker;

    @Transactional
    @Override
    public ProjectResponse createProject(UUID organisationId, ProjectCreateRequest request) throws Exception {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);

        permissionChecker.checkMemberPermission(member, "PROJECTS", "createProject");

        validateOrgBudget(organisation);

        ClientEntity client = clientsRepo.findClientEntitiesByClientIdAndOrganisation(request.getClientId(), organisation)
                .orElseThrow(() -> new ItemNotFoundException("Client does not exist in this organisation"));


        if (projectRepo.existsByNameAndOrganisation(request.getName(), organisation)) {
            throw new ItemNotFoundException("Project with name " + request.getName() + " already exists in this organisation");
        }

        String projectCode = projectCodeSequenceService.generateProjectCode(organisation.getOrganisationId());

        ProjectEntity project = new ProjectEntity();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOrganisation(organisation);
        organisation.addProject(project);
        project.setCreatedBy(member);
        project.setProjectCode(projectCode);
        project.setClient(client);
        project.setContractSum(request.getContractSum() == null ? BigDecimal.ZERO : request.getContractSum());
        project.setContractNumber(request.getContractNumber());
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        ProjectEntity savedProject = projectRepo.save(project);

        OrganisationMember organisationOwner = organisationMemberRepo
                .findByOrganisationAndMemberRole_RoleName(organisation, "OWNER")
                .orElseThrow(() -> new ItemNotFoundException("Organisation owner not found"));

        projectTeamMemberService.addCreatorAndOwnerAsTeamMembers(
                savedProject,
                member,
                organisationOwner
        );

        orgDriveService.createProjectSystemFolder(organisation,savedProject);

        return mapToProjectResponse(savedProject);
    }

    @Override
    public Page<ProjectResponse> getAllProjectsFromOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);

        permissionChecker.checkMemberPermission(member,"PROJECTS","viewProjects");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ProjectEntity> projectPage = projectRepo.findAllByOrganisation(organisation, pageable);

         return projectPage.map(this::mapToProjectResponse);
    }

    @Override
    public ProjectResponse getProjectById(UUID organisationId, UUID projectId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);

        permissionChecker.checkMemberPermission(member,"PROJECTS","viewProjects");

        return mapToProjectResponse(project);
    }

    @Override
    public ProjectResponse updateProject(UUID organisationId, UUID projectId, ProjectUpdateRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        ProjectEntity project = projectRepo.findByOrganisationAndProjectId(organisation, projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, project.getOrganisation());

        if (!validateOrganisationMemberIsTeamMemberInProject(member, project)) {
            throw new AccessDeniedException("Your not a team member in this project");
        }

        permissionChecker.checkMemberPermission(member, "PROJECTS", "updateProject");


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

        if (request.getContractNumber() != null) {
            project.setContractNumber(request.getContractNumber());
        }

        project.setUpdatedAt(LocalDateTime.now());
        ProjectEntity updatedProject = projectRepo.save(project);

        return mapToProjectResponse(updatedProject);
    }

    @Override
    public Boolean deleteProject(UUID organisationId, UUID projectId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        ProjectEntity project = projectRepo.findByOrganisationAndProjectId(organisation, projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project does not exist"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);

        permissionChecker.checkMemberPermission(member, "PROJECTS", "deleteProject");

        project.setStatus(ProjectStatus.DELETED);
        project.setUpdatedAt(LocalDateTime.now());
        project.setDeletedByMemberId(member.getMemberId());
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

    @Override
    public List<ProjectResponseSummary> getAllProjectsAmBelongingToOrganisationUnpaginated(UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);

        // Get all projects where the member is a team member (excluding deleted projects)
        List<ProjectEntity> projects = projectRepo.findByTeamMembersOrganisationMemberAndStatusNot(
                member, ProjectStatus.DELETED);

        return projects.stream()
                .map(this::getProjectResponseSummary)
                .collect(Collectors.toList());
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
        response.setContractSum(project.getContractSum() == null ? BigDecimal.ZERO : project.getContractSum());
        response.setDescription(project.getDescription());
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

    public ProjectResponseSummary getProjectResponseSummary(ProjectEntity project) {
        ProjectResponseSummary response = new ProjectResponseSummary();
        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setClientName(project.getClient().getName());
        return response;
    }

    private void validateOrgBudget(OrganisationEntity organisation) throws ItemNotFoundException {

        Optional<OrgBudgetEntity> activeBudgetOpt = orgBudgetRepo.findByOrganisationAndStatus(
                organisation,
                OrgBudgetStatus.ACTIVE
        );

        if (activeBudgetOpt.isEmpty()) {
            throw new ItemNotFoundException(
                    "Organization does not have an active budget. Please create and activate a budget before creating projects."
            );
        }

        OrgBudgetEntity activeBudget = activeBudgetOpt.get();

//        if (activeBudget.getTotalBudgetAmount() == null ||
//                activeBudget.getTotalBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
//            throw new ItemNotFoundException(
//                    "Organization budget amount is zero or negative. Please update the budget with a valid amount before creating projects."
//            );
//        }
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }
}