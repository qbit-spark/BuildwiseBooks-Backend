package com.qbitspark.buildwisebackend.approval_service.controller;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.payloads.CreateApprovalFlowRequest;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalFlowService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approval/{organisationId}/flows")
@RequiredArgsConstructor
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createApprovalFlow(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateApprovalFlowRequest request
    ) throws ItemNotFoundException, AccessDeniedException {

        ApprovalFlow flow = approvalFlowService.createApprovalFlow(organisationId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow created successfully",
                        flow
                )
        );
    }

    @PutMapping("/{flowId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateApprovalFlow(
            @PathVariable UUID organisationId,
            @PathVariable UUID flowId,
            @Valid @RequestBody CreateApprovalFlowRequest request
    ) throws ItemNotFoundException, AccessDeniedException {

        ApprovalFlow flow = approvalFlowService.updateApprovalFlow(flowId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow updated successfully",
                        flow
                )
        );
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getApprovalFlowByService(
            @PathVariable UUID organisationId,
            @PathVariable ServiceType serviceName
    ) throws ItemNotFoundException {

        ApprovalFlow flow = approvalFlowService.getApprovalFlowByService(organisationId, serviceName);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow retrieved successfully",
                        flow
                )
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllApprovalFlows(
            @PathVariable UUID organisationId
    ) throws ItemNotFoundException, AccessDeniedException {

        List<ApprovalFlow> flows = approvalFlowService.getAllApprovalFlows(organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flows retrieved successfully",
                        flows
                )
        );
    }

    @DeleteMapping("/{flowId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteApprovalFlow(
            @PathVariable UUID organisationId,
            @PathVariable UUID flowId
    ) throws ItemNotFoundException, AccessDeniedException {

        approvalFlowService.deleteApprovalFlow(flowId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval flow deleted successfully"
                )
        );
    }
}