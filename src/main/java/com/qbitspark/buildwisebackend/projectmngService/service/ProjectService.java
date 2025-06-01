package com.qbitspark.buildwisebackend.projectmngService.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.projectmngService.payloads.*;
import org.springframework.data.domain.Page;
import java.util.UUID;

public interface ProjectService {

    ProjectResponse createProject(ProjectCreateRequest request, UUID creatorMemberId, UUID organisationId) throws ItemNotFoundException;

    ProjectResponse getProjectById(UUID projectId, UUID requesterId) throws ItemNotFoundException;

    ProjectResponse updateProject(UUID projectId, ProjectUpdateRequest request, UUID updaterMemberId) throws ItemNotFoundException;

    String deleteProject(UUID projectId, UUID deleterMemberId) throws ItemNotFoundException;


    // Retrieves a paginated list of projects for an organisation, sorted by the specified field and direction
    Page<ProjectListResponse> getOrganisationProjects(
            UUID organisationId, UUID requesterId, int page, int size, String sortBy, String sortDirection) throws ItemNotFoundException;

    Page<ProjectListResponse> searchProjects(ProjectSearchRequest searchRequest, UUID requesterId) throws ItemNotFoundException;

    ProjectResponse updateProjectTeam(UUID projectId, ProjectTeamUpdateRequest request, UUID updaterMemberId) throws ItemNotFoundException;

    Page<ProjectListResponse> getMemberProjects(UUID memberId, int page, int size) throws ItemNotFoundException;
    ProjectStatisticsResponse getProjectStatistics(UUID organisationId, UUID requesterId) throws ItemNotFoundException;

    ProjectResponse removeTeamMember(UUID projectId, UUID memberToRemoveId, UUID removerMemberId);


}
