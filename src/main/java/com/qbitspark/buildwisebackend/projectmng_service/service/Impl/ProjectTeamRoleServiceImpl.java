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
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.CreateProjectTeamRoleRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.UpdateProjectTeamRoleRequest;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ProjectTeamRoleServiceImpl implements ProjectTeamRoleService {

    private final ProjectTeamRoleRepo projectTeamRoleRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final PermissionCheckerService permissionChecker;

    @Override
    @Transactional
    public List<ProjectTeamRoleEntity> createDefaultProjectTeamRoles(OrganisationEntity organisation) {
        List<ProjectTeamRoleEntity> defaultRoles = new ArrayList<>();

        // Create default project team roles
        String[] defaultRoleNames = {
                "PROJECT_MANAGER", "ENGINEER", "ARCHITECT", "LEAD_CONSULTANT", "MEMBER"
        };

        String[] descriptions = {
                "Project Manager - Manages project activities and team",
                "Engineer - Technical engineering role",
                "Architect - System and solution architect",
                "Lead Consultant - Senior consulting role",
                "Developer - Software development role",
                "Accountant - Financial and accounting tasks",
                "Member - General team member"
        };

        for (int i = 0; i < defaultRoleNames.length; i++) {
            ProjectTeamRoleEntity role = new ProjectTeamRoleEntity();
            role.setOrganisation(organisation);
            role.setRoleName(defaultRoleNames[i]);
            role.setDescription(descriptions[i]);
            role.setIsDefaultRole(true);
            role.setIsActive(true);
            role.setCreatedBy(organisation.getOwner().getId());
            role.setCreatedDate(LocalDateTime.now());

            defaultRoles.add(role);
        }

        return projectTeamRoleRepo.saveAll(defaultRoles);
    }

    @Override
    public List<ProjectTeamRoleEntity> getActiveProjectTeamRoles(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {
        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "PROJECTS", "viewProjects");

        return projectTeamRoleRepo.findByOrganisationAndIsActiveTrue(organisation);
    }

    @Override
    @Transactional
    public ProjectTeamRoleEntity createProjectTeamRole(UUID organisationId, CreateProjectTeamRoleRequest request) throws ItemNotFoundException, AccessDeniedException {
        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        if (projectTeamRoleRepo.existsByOrganisationAndRoleNameIgnoreCase(organisation, request.getRoleName())) {
            throw new ItemNotFoundException("Project team role with name '" + request.getRoleName() + "' already exists");
        }

        ProjectTeamRoleEntity role = new ProjectTeamRoleEntity();
        role.setOrganisation(organisation);
        role.setRoleName(request.getRoleName().trim().toUpperCase());
        role.setDescription(request.getDescription());
        role.setIsDefaultRole(false);
        role.setIsActive(true);
        role.setCreatedBy(member.getMemberId());
        role.setCreatedDate(LocalDateTime.now());

        return projectTeamRoleRepo.save(role);
    }

    @Override
    @Transactional
    public ProjectTeamRoleEntity updateProjectTeamRole(UUID roleId, UpdateProjectTeamRoleRequest request) throws ItemNotFoundException, AccessDeniedException {
        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectTeamRoleEntity role = projectTeamRoleRepo.findById(roleId)
                .orElseThrow(() -> new ItemNotFoundException("Project team role not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, role.getOrganisation());
        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        if (request.getRoleName() != null && !request.getRoleName().trim().isEmpty()) {
            String newRoleName = request.getRoleName().trim().toUpperCase();
            if (!role.getRoleName().equals(newRoleName) &&
                    projectTeamRoleRepo.existsByOrganisationAndRoleNameIgnoreCase(role.getOrganisation(), newRoleName)) {
                throw new ItemNotFoundException("Project team role with name '" + newRoleName + "' already exists");
            }
            role.setRoleName(newRoleName);
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription().trim());
        }


        if (request.getIsActive() != null) {
            role.setIsActive(request.getIsActive());
        }

        role.setUpdatedDate(LocalDateTime.now());
        return projectTeamRoleRepo.save(role);
    }

    @Override
    @Transactional
    public boolean deleteProjectTeamRole(UUID roleId) throws ItemNotFoundException, AccessDeniedException {
        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectTeamRoleEntity role = projectTeamRoleRepo.findById(roleId)
                .orElseThrow(() -> new ItemNotFoundException("Project team role not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, role.getOrganisation());
        permissionChecker.checkMemberPermission(member, "PROJECTS", "manageTeam");

        if (role.getIsDefaultRole()) {
            throw new AccessDeniedException("Cannot delete default project team roles");
        }

        // Soft delete by setting active to false
        role.setIsActive(false);
        role.setUpdatedDate(LocalDateTime.now());
        projectTeamRoleRepo.save(role);

        return true;
    }

    @Override
    public ProjectTeamRoleEntity getProjectTeamRoleByName(OrganisationEntity organisation, String roleName) throws ItemNotFoundException {
        return projectTeamRoleRepo.findByOrganisationAndRoleName(organisation, roleName)
                .orElseThrow(() -> new ItemNotFoundException("Project team role '" + roleName + "' not found"));
    }

    @Override
    public ProjectTeamRoleEntity getDefaultProjectTeamRole(OrganisationEntity organisation) throws ItemNotFoundException {
        List<ProjectTeamRoleEntity> defaultRoles = projectTeamRoleRepo.findByOrganisationAndIsActiveTrueAndIsDefaultRoleTrue(organisation);
        if (defaultRoles.isEmpty()) {
            throw new ItemNotFoundException("No default project team roles found for organisation");
        }
        return defaultRoles.getFirst();
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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member does not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }
}