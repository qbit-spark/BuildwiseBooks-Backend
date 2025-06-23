package com.qbitspark.buildwisebackend.projectmng_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    Page<ProjectTeamMemberResponse> getProjectTeamMembers(UUID projectId, Pageable pageable) throws ItemNotFoundException;

    /**
     * Update a single team member's role
     */
    ProjectTeamMemberResponse updateTeamMemberRole(UUID projectId, UUID memberId, UpdateTeamMemberRoleRequest request) throws ItemNotFoundException;

    /**
     * Check if a member is part of a project team
     */
    boolean isTeamMember(UUID projectId, UUID memberId) throws ItemNotFoundException;

    /**
     * Get available team members who can be added to the project
     * Returns organisation members who are not yet part of the project team
     */
    List<AvailableTeamMemberResponse> getAvailableTeamMembers(UUID projectId) throws ItemNotFoundException;

}
