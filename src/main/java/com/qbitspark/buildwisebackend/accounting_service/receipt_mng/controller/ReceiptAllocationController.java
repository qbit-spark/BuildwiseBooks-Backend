package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.controller;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateReceiptAllocationRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.ReceiptAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.ReceiptAllocationService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/receipt-allocations")
@RequiredArgsConstructor
public class ReceiptAllocationController {

    private final ReceiptAllocationService receiptAllocationService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createReceiptAllocation(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateReceiptAllocationRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        ReceiptAllocationEntity allocation = receiptAllocationService
                .createReceiptAllocation(organisationId, request);

        ReceiptAllocationResponse response = mapToResponse(allocation);

        return new ResponseEntity<>(
                GlobeSuccessResponseBuilder.success("Receipt allocation created successfully", response),
                HttpStatus.CREATED
        );
    }


    private ReceiptAllocationResponse mapToResponse(ReceiptAllocationEntity allocation) {
        ReceiptAllocationResponse response = new ReceiptAllocationResponse();
        response.setAllocationId(allocation.getAllocationId());
        response.setReceiptId(allocation.getReceipt().getReceiptId());
        response.setReceiptNumber(allocation.getReceipt().getReceiptNumber());
        response.setReceiptAmount(allocation.getReceipt().getTotalAmount());
        response.setStatus(allocation.getStatus());
        response.setNotes(allocation.getNotes());

        // Use the calculated methods from entity
        response.setTotalAllocatedAmount(allocation.getTotalAllocatedAmount());
        response.setRemainingAmount(allocation.getRemainingAmount());
        response.setFullyAllocated(allocation.isFullyAllocated());

        response.setRequestedBy(allocation.getRequestedBy());
        response.setCreatedAt(allocation.getCreatedAt());
        response.setApprovedBy(allocation.getApprovedBy());
        response.setApprovedAt(allocation.getApprovedAt());

        // Map allocation details - check for null to avoid NPE
        if (allocation.getAllocationDetails() != null) {
            List<ReceiptAllocationResponse.AllocationDetailResponse> detailResponses =
                    allocation.getAllocationDetails().stream()
                            .map(detail -> {
                                ReceiptAllocationResponse.AllocationDetailResponse detailResponse =
                                        new ReceiptAllocationResponse.AllocationDetailResponse();
                                detailResponse.setDetailId(detail.getDetailId());
                                detailResponse.setAccountId(detail.getAccount().getId());
                                detailResponse.setAccountCode(detail.getAccount().getAccountCode());
                                detailResponse.setAccountName(detail.getAccount().getName());
                                detailResponse.setAllocatedAmount(detail.getAllocatedAmount());
                                detailResponse.setDescription(detail.getDescription());
                                return detailResponse;
                            })
                            .collect(Collectors.toList());

            response.setAllocationDetails(detailResponses);
        } else {
            response.setAllocationDetails(new ArrayList<>());
        }

        return response;
    }

}
