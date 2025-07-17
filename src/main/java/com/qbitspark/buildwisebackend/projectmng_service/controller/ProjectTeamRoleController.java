package com.qbitspark.buildwisebackend.projectmng_service.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;

import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.CreateProjectTeamRoleRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectTeamRoleResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.UpdateProjectTeamRoleRequest;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/{organisationId}/project-team-role")
@RequiredArgsConstructor
public class ProjectTeamRoleController {

    private final ProjectTeamRoleService projectTeamRoleService;

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveProjectTeamRoles(
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        List<ProjectTeamRoleEntity> roles = projectTeamRoleService.getActiveProjectTeamRoles(organisationId);
        List<ProjectTeamRoleResponse> responses = roles.stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Active project team roles retrieved successfully",
                        responses
                )
        );
    }


    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createProjectTeamRole(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateProjectTeamRoleRequest request) throws ItemNotFoundException, AccessDeniedException {

        ProjectTeamRoleEntity role = projectTeamRoleService.createProjectTeamRole(organisationId, request);
        ProjectTeamRoleResponse response = mapToResponse(role);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project team role created successfully",
                        response
                )
        );
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProjectTeamRole(
            @PathVariable UUID organisationId,
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateProjectTeamRoleRequest request) throws ItemNotFoundException, AccessDeniedException {

        ProjectTeamRoleEntity role = projectTeamRoleService.updateProjectTeamRole(roleId, request);
        ProjectTeamRoleResponse response = mapToResponse(role);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project team role updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProjectTeamRole(
            @PathVariable UUID organisationId,
            @PathVariable UUID roleId) throws ItemNotFoundException, AccessDeniedException {

        boolean deleted = projectTeamRoleService.deleteProjectTeamRole(roleId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project team role deleted successfully"
                )
        );
    }

    private ProjectTeamRoleResponse mapToResponse(ProjectTeamRoleEntity role) {
        ProjectTeamRoleResponse response = new ProjectTeamRoleResponse();
        response.setRoleId(role.getRoleId());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setIsDefaultRole(role.getIsDefaultRole());
        response.setIsActive(role.getIsActive());
        response.setCreatedDate(role.getCreatedDate());
        response.setUpdatedDate(role.getUpdatedDate());
        return response;
    }
}