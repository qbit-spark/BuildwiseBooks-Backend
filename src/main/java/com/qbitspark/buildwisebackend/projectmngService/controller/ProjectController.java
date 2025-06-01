package com.qbitspark.buildwisebackend.projectmngService.controller;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.projectmngService.payloads.*;
import com.qbitspark.buildwisebackend.projectmngService.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
            @Valid @RequestBody ProjectCreateRequest request) {

        try {
            ProjectResponse projectResponse = projectService.createProject(request, creatorMemberId, organisationId);

            return ResponseEntity.ok(
                    GlobeSuccessResponseBuilder.success(
                            "Project created successfully",
                            projectResponse
                    )
            );

        } catch (ItemNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                            .message(e.getMessage())
                            .build());
        } catch (DataIntegrityViolationException e) {
            // Handle database constraint violations (like budget overflow)
            String errorMessage = "Data validation error: ";
            if (e.getMessage().contains("numeric field overflow")) {
                errorMessage += "Budget value is too large. Maximum supported value is 9,999,999,999,999.99";
            } else {
                errorMessage += "Invalid data provided";
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.BAD_REQUEST)
                            .message(errorMessage)
                            .build());
        } catch (Exception e) {
            // Handle any other unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("An unexpected error occurred while creating the project")
                            .build());
        }
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

    // Soft deletes a project by marking it as cancelled
    @DeleteMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProject(
            @PathVariable UUID projectId,
            @RequestHeader("X-Member-Id") UUID deleterMemberId) throws ItemNotFoundException {
        String response = projectService.deleteProject(projectId, deleterMemberId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(response, null));
    }

    // Retrieves paginated projects for an organisation
    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationProjects(
            @PathVariable UUID organisationId,
            @RequestHeader(value = "X-Member-Id", required = false) UUID requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) throws ItemNotFoundException {

        // If requesterId is null, you might want to get it from authentication context
        if (requesterId == null) {
            // Get from authentication context or return error
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.BAD_REQUEST)
                            .message("Member is not authenticated")
                            .build());
        }

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

    // Removes a specific team member from a project
    @DeleteMapping("/{projectId}/team/{memberToRemoveId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeTeamMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberToRemoveId,
            @RequestHeader("X-Member-Id") UUID removerMemberId) throws ItemNotFoundException {
        ProjectResponse response = projectService.removeTeamMember(projectId, memberToRemoveId, removerMemberId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Team member removed successfully", response));
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