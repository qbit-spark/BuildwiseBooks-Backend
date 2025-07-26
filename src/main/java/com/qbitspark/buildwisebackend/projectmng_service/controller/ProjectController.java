package com.qbitspark.buildwisebackend.projectmng_service.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
//Todo: Changed
@RequestMapping("/api/v1/projects/{organisationId}")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    //Todo: Changed
    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createProject(
            @PathVariable UUID organisationId,
            @Valid @RequestBody ProjectCreateRequest request) throws Exception {

        ProjectResponse projectResponse = projectService.createProject(organisationId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project created successfully",
                        projectResponse
                )
        );
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectById(
            @PathVariable UUID projectId, @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {
        ProjectResponse response = projectService.getProjectById(organisationId, projectId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project retrieved successfully", response));
    }


    @PutMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProject(
            @PathVariable UUID projectId,
            @PathVariable UUID organisationId,
            @Valid @RequestBody ProjectUpdateRequest request) throws AccessDeniedException, ItemNotFoundException {

        ProjectResponse response = projectService.updateProject(organisationId, projectId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project updated successfully", response));
    }


    @DeleteMapping("/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProject(@PathVariable UUID projectId, @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException, RandomExceptions {
        Boolean deleted = projectService.deleteProject(organisationId, projectId);
        if (deleted) {
            return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project deleted successfully", null));
        } else {
            throw new RandomExceptions("Failed to delete project");
        }
    }


    //Todo: Changed
    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationProjects(
            @PathVariable UUID organisationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException, AccessDeniedException {

        Page<ProjectResponse> response = projectService.getAllProjectsFromOrganisation(organisationId, page, size);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Organisation projects retrieved successfully", response));
    }


    @GetMapping("/my-projects")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyProjectsInOrganisation(
            @PathVariable UUID organisationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ProjectResponse> response = projectService.getAllProjectsAmBelongingToOrganisation(organisationId, page, size);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("My projects retrieved successfully", response));
    }


    //Todo: Changed
    @GetMapping("/my-projects-summary-list")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllMyProjectsInOrganisation(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<ProjectResponseSummary> response = projectService.getAllProjectsAmBelongingToOrganisationUnpaginated(organisationId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("All my projects retrieved successfully", response));
    }
}