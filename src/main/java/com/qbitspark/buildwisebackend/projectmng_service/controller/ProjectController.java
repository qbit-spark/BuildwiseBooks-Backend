package com.qbitspark.buildwisebackend.projectmng_service.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/{organisationId}/create")
    public ResponseEntity<GlobeSuccessResponseBuilder> createProject(
            @PathVariable UUID organisationId,
            @Valid @RequestBody ProjectCreateRequest request) throws Exception {

        ProjectResponse projectResponse = projectService.createProject(request, organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project created successfully",
                        projectResponse
                )
        );
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectById(
            @PathVariable UUID projectId) throws ItemNotFoundException, AccessDeniedException {
        ProjectResponse response = projectService.getProjectById(projectId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project retrieved successfully", response));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectUpdateRequest request) throws AccessDeniedException, ItemNotFoundException {

        ProjectResponse response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project updated successfully", response));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProject(@PathVariable UUID projectId) throws ItemNotFoundException, AccessDeniedException, RandomExceptions {
        Boolean deleted = projectService.deleteProject(projectId);
        if (deleted) {
            return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project deleted successfully", null));
        } else {
            throw new RandomExceptions("Failed to delete project");
        }
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationProjects(
            @PathVariable UUID organisationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ProjectResponse> response = projectService.getAllProjectsFromOrganisation(organisationId, page, size);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Organisation projects retrieved successfully", response));
    }

}