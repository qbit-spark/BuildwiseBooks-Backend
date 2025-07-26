package com.qbitspark.buildwisebackend.projectmng_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
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

    List<ProjectTeamMemberResponse> addTeamMembers(UUID organisationId, UUID projectId, Set<BulkAddTeamMemberRequest> requests) throws Exception;

    void addCreatorAndOwnerAsTeamMembers(ProjectEntity project, OrganisationMember creator, OrganisationMember ownerOfOrganisation) throws Exception;

    ProjectTeamRemovalResponse removeTeamMembers(UUID organisationId,UUID projectId, Set<UUID> memberIds) throws ItemNotFoundException, AccessDeniedException;

    Page<ProjectTeamMemberResponse> getProjectTeamMembers(UUID organisationId, UUID projectId, Pageable pageable) throws ItemNotFoundException, AccessDeniedException;

    ProjectTeamMemberResponse updateTeamMemberRole(UUID organisationId, UUID projectId, UUID memberId, UpdateTeamMemberRoleRequest request) throws ItemNotFoundException, AccessDeniedException;

    boolean isTeamMember(UUID organisationId, UUID projectId, UUID memberId) throws ItemNotFoundException, AccessDeniedException;

    List<AvailableTeamMemberResponse> getAvailableTeamMembers(UUID organisationId, UUID projectId) throws ItemNotFoundException, AccessDeniedException;

}
