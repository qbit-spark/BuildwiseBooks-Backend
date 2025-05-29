package com.qbitspark.buildwisebackend.projectmngService.controller;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.projectmngService.payloads.*;
import com.qbitspark.buildwisebackend.projectmngService.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    // Creates a new project for the specified organisation with the given creator member ID
    @PostMapping("/{organisationId}/create/{creatorMemberId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> createProject(
            @PathVariable UUID organisationId,
            @PathVariable UUID creatorMemberId,
            @Valid @RequestBody ProjectCreateRequest request) throws ItemNotFoundException {
        ProjectResponse response = projectService.createProject(request, creatorMemberId, organisationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.success("Project created successfully", response));
    }

    // Retrieves a project by its ID, ensuring the requester has access
    @GetMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectById(
            @PathVariable UUID projectId,
            @RequestHeader("X-Member-Id") UUID requesterId) throws ItemNotFoundException {
        ProjectResponse response = projectService.getProjectById(projectId, requesterId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project retrieved successfully", response));
    }

    // Updates an existing project's details
    @PutMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectUpdateRequest request,
            @RequestHeader("X-Member-Id") UUID updaterMemberId) throws ItemNotFoundException {
        ProjectResponse response = projectService.updateProject(projectId, request, updaterMemberId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project updated successfully", response));
    }

    // Deletes a project by marking it as cancelled
    @DeleteMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProject(
            @PathVariable UUID projectId,
            @RequestHeader("X-Member-Id") UUID deleterMemberId) throws ItemNotFoundException {
        String response = projectService.deleteProject(projectId, deleterMemberId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project deleted successfully", response));
    }

    // Retrieves paginated projects for an organisation
    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationProjects(
            @PathVariable UUID organisationId,
            @RequestHeader("X-Member-Id") UUID requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) throws ItemNotFoundException {
        Page<ProjectListResponse> response = projectService.getOrganisationProjects(
                organisationId, requesterId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Organisation projects retrieved successfully", response));
    }

    // Searches for projects based on criteria
    @PostMapping("/search")
    public ResponseEntity<GlobeSuccessResponseBuilder> searchProjects(
            @Valid @RequestBody ProjectSearchRequest searchRequest,
            @RequestHeader("X-Member-Id") UUID requesterId) throws ItemNotFoundException {
        Page<ProjectListResponse> response = projectService.searchProjects(searchRequest, requesterId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Projects search completed successfully", response));
    }

    // Updates the team members assigned to a project
    @PutMapping("/{projectId}/team")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProjectTeam(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectTeamUpdateRequest request,
            @RequestHeader("X-Member-Id") UUID updaterMemberId) throws ItemNotFoundException {
        ProjectResponse response = projectService.updateProjectTeam(projectId, request, updaterMemberId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project team updated successfully", response));
    }

    // Retrieves paginated projects for a specific member
    @GetMapping("/member/{memberId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMemberProjects(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {
        Page<ProjectListResponse> response = projectService.getMemberProjects(memberId, page, size);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Member projects retrieved successfully", response));
    }

    // Retrieves statistics for projects in an organisation
    @GetMapping("/organisation/{organisationId}/statistics")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectStatistics(
            @PathVariable UUID organisationId,
            @RequestHeader("X-Member-Id") UUID requesterId) throws ItemNotFoundException {
        ProjectStatisticsResponse response = projectService.getProjectStatistics(organisationId, requesterId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project statistics retrieved successfully", response));
    }

    // Health check endpoint for the project service
    @GetMapping("/health")
    public ResponseEntity<GlobeSuccessResponseBuilder> healthCheck() {
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project service is running", "OK"));
    }
}
