package com.qbitspark.buildwisebackend.approval_service.controller;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStep;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalFlowResponse;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalStepResponse;
import com.qbitspark.buildwisebackend.approval_service.payloads.CreateApprovalFlowRequest;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalFlowService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.OrgMemberRoleRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/approval/{organisationId}/flows")
@RequiredArgsConstructor
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;
    private final OrgMemberRoleRepo orgMemberRoleRepo;
    private final ProjectTeamRoleRepo projectTeamRoleRepo;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createApprovalFlow(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateApprovalFlowRequest request
    ) throws ItemNotFoundException, AccessDeniedException {

        ApprovalFlow flow = approvalFlowService.createApprovalFlow(organisationId, request);
        ApprovalFlowResponse response = mapToResponse(flow);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow created successfully",
                        response
                )
        );
    }

    @PutMapping("/{flowId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateApprovalFlow(
            @PathVariable UUID organisationId,
            @PathVariable UUID flowId,
            @Valid @RequestBody CreateApprovalFlowRequest request
    ) throws ItemNotFoundException, AccessDeniedException {

        ApprovalFlow flow = approvalFlowService.updateApprovalFlow(organisationId, flowId, request);
        ApprovalFlowResponse response = mapToResponse(flow);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow updated successfully",
                        response
                )
        );
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getApprovalFlowByService(
            @PathVariable UUID organisationId,
            @PathVariable ServiceType serviceName
    ) throws ItemNotFoundException {

        ApprovalFlow flow = approvalFlowService.getApprovalFlowByService(organisationId, serviceName);
        ApprovalFlowResponse response = mapToResponse(flow);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow retrieved successfully",
                        response
                )
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllApprovalFlows(
            @PathVariable UUID organisationId
    ) throws ItemNotFoundException, AccessDeniedException {

        List<ApprovalFlow> flows = approvalFlowService.getAllApprovalFlows(organisationId);
        List<ApprovalFlowResponse> responses = flows.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flows retrieved successfully",
                        responses
                )
        );
    }

    @DeleteMapping("/{flowId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteApprovalFlow(
            @PathVariable UUID organisationId,
            @PathVariable UUID flowId
    ) throws ItemNotFoundException, AccessDeniedException {

        approvalFlowService.deleteApprovalFlow(organisationId, flowId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow deleted successfully"
                )
        );
    }

    private ApprovalFlowResponse mapToResponse(ApprovalFlow flow) {
        ApprovalFlowResponse response = new ApprovalFlowResponse();
        response.setFlowId(flow.getFlowId());
        response.setServiceName(flow.getServiceName());
        response.setDescription(flow.getDescription());
        response.setOrganisationId(flow.getOrganisation().getOrganisationId());
        response.setOrganisationName(flow.getOrganisation().getOrganisationName());
        response.setActive(flow.getIsActive());
        response.setTotalSteps(flow.getSteps() != null ? flow.getSteps().size() : 0);
        response.setCreatedAt(flow.getCreatedAt());
        response.setUpdatedAt(flow.getUpdatedAt());

        if (flow.getSteps() != null) {
            List<ApprovalStepResponse> stepResponses = flow.getSteps().stream()
                    .map(this::mapToStepResponse)
                    .collect(Collectors.toList());
            response.setSteps(stepResponses);
        }

        return response;
    }

    private ApprovalStepResponse mapToStepResponse(ApprovalStep step) {
        ApprovalStepResponse response = new ApprovalStepResponse();
        response.setStepId(step.getStepId());
        response.setStepOrder(step.getStepOrder());
        response.setScopeType(step.getScopeType());
        response.setRoleId(step.getRoleId());
        response.setRequired(step.isRequired());

        // Get role name based on scope type
        String roleName = getRoleName(step.getRoleId(), step.getScopeType());
        response.setRoleName(roleName);

        return response;
    }

    private String getRoleName(UUID roleId, ScopeType scopeType) {
        try {
            switch (scopeType) {
                case ORGANIZATION -> {
                    Optional<OrgMemberRoleEntity> orgRole = orgMemberRoleRepo.findById(roleId);
                    return orgRole.map(OrgMemberRoleEntity::getRoleName).orElse("Unknown Role");
                }
                case PROJECT -> {
                    Optional<ProjectTeamRoleEntity> projectRole = projectTeamRoleRepo.findById(roleId);
                    return projectRole.map(ProjectTeamRoleEntity::getRoleName).orElse("Unknown Role");
                }
                default -> {
                    return "Unknown Role";
                }
            }
        } catch (Exception e) {
            return "Unknown Role";
        }
    }
}