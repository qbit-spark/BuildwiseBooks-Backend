package com.qbitspark.buildwisebackend.projectmng_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.BulkAddTeamMemberRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectTeamMemberResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectTeamRemovalResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.UpdateTeamMemberRoleRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ProjectTeamMemberService {

    /**
     * Add team members in the bulk - same role for multiple members
     */
    List<ProjectTeamMemberResponse> addTeamMembers(UUID projectId, Set<BulkAddTeamMemberRequest> requests) throws Exception;

    void addCreatorAndOwnerAsTeamMembers(ProjectEntity project, OrganisationMember creator, OrganisationMember ownerOfOrganisation) throws Exception;

    /**
     * Remove multiple team members at once
     */
    ProjectTeamRemovalResponse removeTeamMembers(UUID projectId, Set<UUID> memberIds) throws ItemNotFoundException;

    /**
     * Get all team members for a project
     */
    List<ProjectTeamMemberResponse> getProjectTeamMembers(UUID projectId) throws ItemNotFoundException;

    /**
     * Update a single team member's role
     */
    ProjectTeamMemberResponse updateTeamMemberRole(UUID projectId, UUID memberId, UpdateTeamMemberRoleRequest request) throws ItemNotFoundException;

    /**
     * Check if a member is part of a project team
     */
    boolean isTeamMember(UUID projectId, UUID memberId) throws ItemNotFoundException;

}
