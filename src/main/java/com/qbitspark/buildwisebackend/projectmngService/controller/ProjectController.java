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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("An unexpected error occurred while creating the project")
                            .build());
        }
    }

    @GetMapping("/{projectId}/{requesterId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectById(
            @PathVariable UUID projectId,
            @PathVariable UUID requesterId) {
        try {
            ProjectResponse response = projectService.getProjectById(projectId, requesterId);
            return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project retrieved successfully", response));
        } catch (ItemNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.NOT_FOUND)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/{organisationId}/{projectId}/{updaterMemberId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProject(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @PathVariable UUID updaterMemberId,
            @Valid @RequestBody ProjectUpdateRequest request) {
        try {
            ProjectResponse response = projectService.updateProject(projectId, organisationId, request, updaterMemberId);
            return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project updated successfully", response));
        } catch (ItemNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                            .message(e.getMessage())
                            .build());
        } catch (DataIntegrityViolationException e) {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .message("An unexpected error occurred while updating the project")
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Page<ProjectListResponse> response = projectService.getAllProjects(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("All projects retrieved successfully", response));
    }

    @DeleteMapping("/{projectId}/{deleterMemberId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProject(
            @PathVariable UUID projectId,
            @PathVariable UUID deleterMemberId) {
        try {
            String response = projectService.deleteProject(projectId, deleterMemberId);
            return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(response, null));
        } catch (ItemNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.NOT_FOUND)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationProjects(
            @PathVariable UUID organisationId,
            @RequestHeader(value = "X-Member-Id", required = false) UUID requesterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) throws ItemNotFoundException {

        if (requesterId == null) {
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

    @PostMapping("/search")
    public ResponseEntity<GlobeSuccessResponseBuilder> searchProjects(
            @Valid @RequestBody ProjectSearchRequest searchRequest,
            @RequestHeader(value = "X-Member-Id", required = false) UUID requesterId) throws ItemNotFoundException {
        Page<ProjectListResponse> response = projectService.searchProjects(searchRequest, requesterId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Projects search completed successfully", response));
    }

    @PutMapping("/{projectId}/team/{updaterMemberId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProjectTeam(
            @PathVariable UUID projectId,
            @PathVariable UUID updaterMemberId,
            @Valid @RequestBody ProjectTeamUpdateRequest request) {
        try {
            ProjectResponse response = projectService.updateProjectTeam(projectId, request, updaterMemberId);
            return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project team updated successfully", response));
        } catch (ItemNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GlobeSuccessResponseBuilder.builder()
                            .success(false)
                            .httpStatus(HttpStatus.NOT_FOUND)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/{projectId}/team/{memberToRemoveId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeTeamMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberToRemoveId,
            @RequestHeader(value = "X-Member-Id", required = false) UUID removerMemberId) throws ItemNotFoundException {
        ProjectResponse response = projectService.removeTeamMember(projectId, memberToRemoveId, removerMemberId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Team member removed successfully", response));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMemberProjects(
            @PathVariable UUID memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {
        Page<ProjectListResponse> response = projectService.getMemberProjects(memberId, page, size);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Member projects retrieved successfully", response));
    }

    @GetMapping("/organisation/{organisationId}/statistics")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectStatistics(
            @PathVariable UUID organisationId,
            @RequestHeader(value = "X-Member-Id", required = false) UUID requesterId) throws ItemNotFoundException {
        ProjectStatisticsResponse response = projectService.getProjectStatistics(organisationId, requesterId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project statistics retrieved successfully", response));
    }

    @GetMapping("/health")
    public ResponseEntity<GlobeSuccessResponseBuilder> healthCheck() {
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project service is running", "OK"));
    }
}