package com.qbitspark.buildwisebackend.organisation_service.roles_mng.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.MemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.CreateRoleRequest;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.UpdateRoleRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MemberRoleService {

    List<MemberRoleEntity> createDefaultRolesForOrganisation(OrganisationEntity organisation);

    List<MemberRoleEntity> getAllRolesForOrganisation(OrganisationEntity organisation);

    MemberRoleEntity getMemberRole(OrganisationEntity organisation);

    MemberRoleEntity createNewRole(UUID organisationId, CreateRoleRequest createRoleRequest) throws ItemNotFoundException;

    MemberRoleEntity updateRole(UUID roleId, UpdateRoleRequest updateRoleRequest) throws AccessDeniedException, ItemNotFoundException;

    MemberRoleEntity assignRoleToMember(OrganisationMember member, String roleName);
}