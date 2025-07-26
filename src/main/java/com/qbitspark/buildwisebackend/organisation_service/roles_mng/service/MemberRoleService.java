package com.qbitspark.buildwisebackend.organisation_service.roles_mng.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.CreateRoleRequest;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.UpdateRoleRequest;

import java.util.List;
import java.util.UUID;

public interface MemberRoleService {

    List<OrgMemberRoleEntity> createDefaultRolesForOrganisation(OrganisationEntity organisation);

    List<OrgMemberRoleEntity> getAllRolesForOrganisation(OrganisationEntity organisation);

    OrgMemberRoleEntity getMemberRole(OrganisationEntity organisation);

    OrgMemberRoleEntity createNewRole(UUID organisationId, CreateRoleRequest createRoleRequest) throws ItemNotFoundException;

    OrgMemberRoleEntity updateRole(UUID roleId, UpdateRoleRequest updateRoleRequest) throws AccessDeniedException, ItemNotFoundException;

    OrgMemberRoleEntity assignRoleToMember(OrganisationMember member, String roleName);
}