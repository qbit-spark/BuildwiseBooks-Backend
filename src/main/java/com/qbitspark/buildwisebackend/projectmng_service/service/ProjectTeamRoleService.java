package com.qbitspark.buildwisebackend.projectmng_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.CreateProjectTeamRoleRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.UpdateProjectTeamRoleRequest;


import java.util.List;
import java.util.UUID;

public interface ProjectTeamRoleService {

    List<ProjectTeamRoleEntity> createDefaultProjectTeamRoles(OrganisationEntity organisation);
    List<ProjectTeamRoleEntity> getActiveProjectTeamRoles(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    ProjectTeamRoleEntity createProjectTeamRole(UUID organisationId, CreateProjectTeamRoleRequest request) throws ItemNotFoundException, AccessDeniedException;

    ProjectTeamRoleEntity updateProjectTeamRole(UUID roleId, UpdateProjectTeamRoleRequest request) throws ItemNotFoundException, AccessDeniedException;

    boolean deleteProjectTeamRole(UUID roleId) throws ItemNotFoundException, AccessDeniedException;

    ProjectTeamRoleEntity getProjectTeamRoleByName(OrganisationEntity organisation, String roleName) throws ItemNotFoundException;

    ProjectTeamRoleEntity getDefaultProjectTeamRole(OrganisationEntity organisation) throws ItemNotFoundException;

}