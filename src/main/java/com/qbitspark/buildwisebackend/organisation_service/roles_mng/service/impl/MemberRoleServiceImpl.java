package com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.impl;


import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.config.PermissionConfig;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.CreateRoleRequest;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.UpdateRoleRequest;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.MemberRoleRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.MemberRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberRoleServiceImpl implements MemberRoleService {

    private final MemberRoleRepo memberRoleRepository;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final OrganisationRepo organisationRepo;
    private final PermissionCheckerServiceImpl permissionChecker;

    @Override
    @Transactional
    public List<OrgMemberRoleEntity> createDefaultRolesForOrganisation(OrganisationEntity organisation) {

        List<OrgMemberRoleEntity> defaultRoles = new ArrayList<>();

        // Create an OWNER role
        OrgMemberRoleEntity ownerRole = new OrgMemberRoleEntity();
        ownerRole.setOrganisation(organisation);
        ownerRole.setRoleName("OWNER");
        ownerRole.setIsDefaultRole(true);
        ownerRole.setIsActive(true);
        ownerRole.setPermissions(createOwnerPermissions());
        ownerRole.setCreatedBy(organisation.getOwner().getId());
        ownerRole.setCreatedDate(LocalDateTime.now());
        defaultRoles.add(ownerRole);

        // Create ADMIN role
        OrgMemberRoleEntity adminRole = new OrgMemberRoleEntity();
        adminRole.setOrganisation(organisation);
        adminRole.setRoleName("ADMIN");
        adminRole.setIsDefaultRole(true);
        adminRole.setIsActive(true);
        adminRole.setPermissions(createAdminPermissions());
        adminRole.setCreatedBy(organisation.getOwner().getId());
        adminRole.setCreatedDate(LocalDateTime.now());
        defaultRoles.add(adminRole);

        // Create a MEMBER role
        OrgMemberRoleEntity memberRole = new OrgMemberRoleEntity();
        memberRole.setOrganisation(organisation);
        memberRole.setRoleName("MEMBER");
        memberRole.setIsDefaultRole(true);
        memberRole.setIsActive(true);
        memberRole.setPermissions(createMemberPermissions());
        memberRole.setCreatedBy(organisation.getOwner().getId());
        memberRole.setCreatedDate(LocalDateTime.now());
        defaultRoles.add(memberRole);


        return memberRoleRepository.saveAll(defaultRoles);
    }

    @Override
    public List<OrgMemberRoleEntity> getAllRolesForOrganisation(OrganisationEntity organisation) {
        return memberRoleRepository.findByOrganisationAndIsActiveTrue(organisation);
    }

    @Override
    public OrgMemberRoleEntity getMemberRole(OrganisationEntity organisation) {
        return memberRoleRepository.findByOrganisationAndRoleName(organisation, "MEMBER")
                .orElseThrow(() -> new RuntimeException("MEMBER role not found for organisation"));
    }

    @Override
    @Transactional
    public OrgMemberRoleEntity createNewRole(UUID organisationId, CreateRoleRequest createRoleRequest) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember organisationMember = validateOrganisationMemberAccess(currentUser, organisation);

        if (memberRoleRepository.existsByOrganisationAndRoleNameIgnoreCase(organisation,createRoleRequest.getName())){
            throw new ItemNotFoundException("Role with name " + createRoleRequest.getName() + " already exists");
        }

        OrgMemberRoleEntity newRole = new OrgMemberRoleEntity();
        newRole.setOrganisation(organisation);
        newRole.setRoleName(createRoleRequest.getName());
        newRole.setIsDefaultRole(false);
        newRole.setIsActive(true);
        newRole.setPermissions(validatePermissions(createRoleRequest.getPermissions()));
        newRole.setCreatedBy(organisationMember.getMemberId());
        newRole.setCreatedDate(LocalDateTime.now());

        return memberRoleRepository.save(newRole);
    }

    @Override
    @Transactional
    public OrgMemberRoleEntity updateRole(UUID roleId, UpdateRoleRequest updateRoleRequest) throws AccessDeniedException, ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrgMemberRoleEntity role = memberRoleRepository.findById(roleId)
                .orElseThrow(() -> new ItemNotFoundException("Role not found"));

        OrganisationEntity organisation = role.getOrganisation();
        OrganisationMember organisationMember = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(organisationMember, "ORGANISATION", "manageMembers");

        if (role.getIsDefaultRole()) {
            throw new IllegalArgumentException("Cannot update default role");
        }

        if (updateRoleRequest.getName() != null && !updateRoleRequest.getName().trim().isEmpty()) {
            String newName = updateRoleRequest.getName().trim();

                if (memberRoleRepository.existsByOrganisationAndRoleNameIgnoreCase(organisation, newName)) {
                    throw new IllegalArgumentException("Role with name '" + newName + "' already exists in this organisation");
                }
                role.setRoleName(newName);
        }

        if (updateRoleRequest.getDescription() != null) {
            role.setDescription(updateRoleRequest.getDescription().trim());
        }

        Map<String, Map<String, Boolean>> validatedPermissions = validatePermissions(updateRoleRequest.getPermissions());
        role.setPermissions(validatedPermissions);
        role.setUpdatedDate(LocalDateTime.now());

        return memberRoleRepository.save(role);
    }

    @Override
    @Transactional
    public OrgMemberRoleEntity assignRoleToMember(OrganisationMember member, String roleName) {

        OrgMemberRoleEntity role = memberRoleRepository.findByOrganisationAndRoleName(member.getOrganisation(), roleName)
                .orElseThrow(() -> new RuntimeException("Role '" + roleName + "' not found"));

        member.setMemberRole(role);
        organisationMemberRepo.save(member);

        return role;

    }

    // Permission creation methods
    private Map<String, Map<String, Boolean>> createOwnerPermissions() {
        // Owner gets all permissions
        Map<String, Map<String, Boolean>> permissions = PermissionConfig.getAllPermissions();
        permissions.forEach((resource, resourcePermissions) ->
                resourcePermissions.replaceAll((permission, value) -> true)
        );
        return permissions;
    }

    private Map<String, Map<String, Boolean>> createAdminPermissions() {
        // Admin gets most permissions except some sensitive ones
        Map<String, Map<String, Boolean>> permissions = PermissionConfig.getAllPermissions();
        permissions.forEach((resource, resourcePermissions) -> {
            resourcePermissions.replaceAll((permission, value) -> true);

            // Remove sensitive permissions
            if (resource.equals(PermissionConfig.ORGANISATION)) {
                resourcePermissions.put("deleteOrganisation", false);
            }
            if (resource.equals(PermissionConfig.SYSTEM)) {
                resourcePermissions.put("manageSystemSettings", false);
            }
        });
        return permissions;
    }

    private Map<String, Map<String, Boolean>> createMemberPermissions() {
        // Member gets basic permissions
        Map<String, Map<String, Boolean>> permissions = PermissionConfig.getAllPermissions();

        // Only grant specific permissions for members
        permissions.get(PermissionConfig.ORGANISATION).put("viewOrganisation", true);
        permissions.get(PermissionConfig.ORGANISATION).put("viewMembers", true);

        permissions.get(PermissionConfig.PROJECTS).put("viewProjects", true);
        permissions.get(PermissionConfig.PROJECTS).put("createProject", true);
        permissions.get(PermissionConfig.PROJECTS).put("viewTeamMembers", true);

        permissions.get(PermissionConfig.CLIENTS).put("viewClients", true);
        permissions.get(PermissionConfig.CLIENTS).put("createClient", true);

        permissions.get(PermissionConfig.DRIVE).put("uploadFiles", true);
        permissions.get(PermissionConfig.DRIVE).put("viewFiles", true);
        permissions.get(PermissionConfig.DRIVE).put("downloadFiles", true);

        return permissions;
    }


    private Map<String, Map<String, Boolean>> validatePermissions(Map<String, Map<String, Boolean>> inputPermissions) {
        validateInput(inputPermissions);

        Map<String, Map<String, Boolean>> systemPermissions = PermissionConfig.getAllPermissions();
        Map<String, Map<String, Boolean>> validatedPermissions = new HashMap<>();

        inputPermissions.forEach((resource, resourcePermissions) -> {
            validateResource(resource, resourcePermissions, systemPermissions);
            validatedPermissions.put(resource, validateAndMergePermissions(
                    resourcePermissions, systemPermissions.get(resource), resource));
        });

        return validatedPermissions;
    }

    private void validateInput(Map<String, Map<String, Boolean>> inputPermissions) {
        if (inputPermissions == null) {
            throw new IllegalArgumentException("Permissions cannot be null");
        }
        if (inputPermissions.isEmpty()) {
            throw new IllegalArgumentException("At least one resource permission must be provided");
        }
    }

    private void validateResource(String resource, Map<String, Boolean> resourcePermissions,
                                  Map<String, Map<String, Boolean>> systemPermissions) {
        if (!systemPermissions.containsKey(resource)) {
            throw new IllegalArgumentException("Invalid resource detected: " + resource);
        }
        if (resourcePermissions == null) {
            throw new IllegalArgumentException("Resource '" + resource + "' permissions cannot be null");
        }
        if (resourcePermissions.isEmpty()) {
            throw new IllegalArgumentException("Resource '" + resource + "' must have at least one permission");
        }
    }

    private Map<String, Boolean> validateAndMergePermissions(Map<String, Boolean> resourcePermissions,
                                                             Map<String, Boolean> systemResourcePermissions,
                                                             String resource) {
        Map<String, Boolean> result = new HashMap<>(systemResourcePermissions);

        // Set all permissions to false initially
        result.replaceAll((k, v) -> false);

        // Validate and set provided permissions
        for (Map.Entry<String, Boolean> permEntry : resourcePermissions.entrySet()) {
            String permission = permEntry.getKey();
            Boolean value = permEntry.getValue();

            if (!systemResourcePermissions.containsKey(permission)) {
                throw new IllegalArgumentException("Invalid permission '" + permission + "' for resource '" + resource + "'");
            }
            if (value == null) {
                throw new IllegalArgumentException("Permission '" + permission + "' value cannot be null");
            }

            result.put(permission, value);
        }

        return result;
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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }
}