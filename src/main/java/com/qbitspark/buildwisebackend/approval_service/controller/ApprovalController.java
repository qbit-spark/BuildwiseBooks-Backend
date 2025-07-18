package com.qbitspark.buildwisebackend.approval_service.controller;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionRequest;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalActionResponse;
import com.qbitspark.buildwisebackend.approval_service.payloads.ApprovalHistoryResponse;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalActionService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalHistoryService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalStatusService;
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
@RequestMapping("/api/v1/approval/{organisationId}")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalHistoryService approvalHistoryService;
    private final ApprovalActionService approvalActionService;
    private final ApprovalStatusService approvalStatusService;

    // Get approval history for specific item
    @GetMapping("/history/{serviceType}/{itemId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getApprovalHistory(
            @PathVariable UUID organisationId,
            @PathVariable ServiceType serviceType,
            @PathVariable UUID itemId) throws ItemNotFoundException {

        ApprovalHistoryResponse history = approvalHistoryService.getApprovalHistory(serviceType, itemId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Approval history retrieved successfully",
                        history
                )
        );
    }

    // Take approval action (approve/reject)
    @PostMapping("/action/{serviceType}/{itemId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> takeApprovalAction(
            @PathVariable UUID organisationId,
            @PathVariable ServiceType serviceType,
            @PathVariable UUID itemId,
            @Valid @RequestBody ApprovalActionRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        ApprovalActionResponse response = approvalActionService.takeApprovalAction(
                serviceType, itemId, request);

        String message = request.getAction().name().toLowerCase() + " action completed successfully";

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(message, response)
        );
    }

    // Get my pending approvals
    @GetMapping("/my-pending")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyPendingApprovals(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<ApprovalInstance> pendingApprovals = approvalStatusService.getMyPendingApprovals();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Pending approvals retrieved successfully",
                        pendingApprovals
                )
        );
    }
}