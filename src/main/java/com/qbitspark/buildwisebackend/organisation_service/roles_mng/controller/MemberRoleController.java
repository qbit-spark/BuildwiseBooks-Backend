package com.qbitspark.buildwisebackend.organisation_service.roles_mng.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.CreateRoleRequest;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.CreateRoleResponse;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.UpdateRoleRequest;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.payload.UpdateRoleResponse;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.MemberRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/member-roles/{organisationId}")
@RequiredArgsConstructor
public class MemberRoleController {

    private final MemberRoleService memberRoleService;
    private final OrganisationRepo organisationRepo;

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllMemberRoles(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        List<OrgMemberRoleEntity> roles = memberRoleService.getAllRolesForOrganisation(organisation);
        List<CreateRoleResponse> responses = roles.stream()
                .map(this::mapToCreateRoleResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Member roles retrieved successfully",
                        responses
                )
        );
    }


    @GetMapping("/summary-list")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllMemberRolesSummaryList(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        List<OrgMemberRoleEntity> roles = memberRoleService.getAllRolesForOrganisation(organisation);
        List<CreateRoleResponse> responses = roles.stream()
                .map(this::mapToSummaryRoleResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Member roles retrieved successfully",
                        responses
                )
        );
    }


    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createMemberRole(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateRoleRequest request) throws ItemNotFoundException, AccessDeniedException {

        OrgMemberRoleEntity role = memberRoleService.createNewRole(organisationId, request);
        CreateRoleResponse response = mapToCreateRoleResponse(role);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Member role created successfully",
                        response
                )
        );
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateMemberRole(
            @PathVariable UUID organisationId,
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request) throws ItemNotFoundException, AccessDeniedException {

        OrgMemberRoleEntity role = memberRoleService.updateRole(roleId, request);
        UpdateRoleResponse response = mapToUpdateRoleResponse(role);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Member role updated successfully",
                        response
                )
        );
    }

    @GetMapping("/default")
    public ResponseEntity<GlobeSuccessResponseBuilder> getDefaultMemberRole(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrgMemberRoleEntity defaultRole = memberRoleService.getMemberRole(organisation);
        CreateRoleResponse response = mapToCreateRoleResponse(defaultRole);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Default member role retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMemberRoleById(
            @PathVariable UUID organisationId,
            @PathVariable UUID roleId) throws ItemNotFoundException {

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        List<OrgMemberRoleEntity> roles = memberRoleService.getAllRolesForOrganisation(organisation);
        OrgMemberRoleEntity role = roles.stream()
                .filter(r -> r.getRoleId().equals(roleId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Member role not found"));

        CreateRoleResponse response = mapToCreateRoleResponse(role);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Member role retrieved successfully",
                        response
                )
        );
    }

    // Helper mapping methods
    private CreateRoleResponse mapToCreateRoleResponse(OrgMemberRoleEntity role) {
        CreateRoleResponse response = new CreateRoleResponse();
        response.setId(role.getRoleId().toString());
        response.setName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setPermissions(role.getPermissions());
        response.setCreatedAt(role.getCreatedDate());
        // You might want to get the creator's name from the Account entity
        response.setCreatedBy(role.getCreatedBy() != null ? role.getCreatedBy().toString() : "System");
        return response;
    }


    private CreateRoleResponse mapToSummaryRoleResponse(OrgMemberRoleEntity role) {
        CreateRoleResponse response = new CreateRoleResponse();
        response.setId(role.getRoleId().toString());
        response.setName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setCreatedAt(role.getCreatedDate());
        // You might want to get the creator's name from the Account entity
        response.setCreatedBy(role.getCreatedBy() != null ? role.getCreatedBy().toString() : "System");
        return response;
    }


    private UpdateRoleResponse mapToUpdateRoleResponse(OrgMemberRoleEntity role) {
        UpdateRoleResponse response = new UpdateRoleResponse();
        response.setId(role.getRoleId().toString());
        response.setName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setPermissions(role.getPermissions());
        response.setUpdatedAt(role.getUpdatedDate());
        response.setUpdatedBy(role.getCreatedBy() != null ? role.getCreatedBy().toString() : "System");
        response.setIsDefaultRole(role.getIsDefaultRole());
        return response;
    }
}