package com.qbitspark.buildwisebackend.projectmng_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ProjectService {
    ProjectResponse createProject(UUID organisationId, ProjectCreateRequest request) throws Exception;
    Page<ProjectResponse> getAllProjectsFromOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException, AccessDeniedException;
    ProjectResponse getProjectById(UUID organisationId, UUID projectId) throws ItemNotFoundException, AccessDeniedException;
    ProjectResponse updateProject(UUID organisationId, UUID projectId, ProjectUpdateRequest request) throws ItemNotFoundException, AccessDeniedException;
    Boolean deleteProject(UUID organisationId, UUID projectId)  throws ItemNotFoundException, AccessDeniedException;
    Page<ProjectResponse> getAllProjectsAmBelongingToOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException;
    List<ProjectResponseSummary> getAllProjectsAmBelongingToOrganisationUnpaginated(UUID organisationId) throws ItemNotFoundException;
}