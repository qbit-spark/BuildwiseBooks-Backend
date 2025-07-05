package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.controller;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.*;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetAllocationService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/budgets/{budgetId}/allocations")
@RequiredArgsConstructor
public class OrgBudgetAllocationController {

    private final OrgBudgetAllocationService allocationService;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final OrganisationRepo organisationRepo;


    @PostMapping("/allocate")
    public ResponseEntity<GlobeSuccessResponseBuilder> allocateMoneyToDetailAccounts(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody AllocateMoneyRequest request) throws ItemNotFoundException {

        List<OrgBudgetDetailAllocationEntity> allocations = allocationService.allocateMoneyToDetailAccounts(
                organisationId, budgetId, request);

        List<DetailAllocationResponse> responses = allocations.stream()
                .map(this::mapToAllocationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Money allocated to detail accounts successfully", responses));
    }


    @GetMapping("/header/{headerLineItemId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getHeaderAllocations(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @PathVariable UUID headerLineItemId) throws ItemNotFoundException {

        List<OrgBudgetDetailAllocationEntity> allocations = allocationService.getHeaderAllocations(
                organisationId, budgetId, headerLineItemId);

        List<DetailAllocationResponse> responses = allocations.stream()
                .map(this::mapToAllocationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Header allocations retrieved successfully", responses));
    }

    @GetMapping("/header/{headerLineItemId}/summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getHeaderAllocationSummary(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @PathVariable UUID headerLineItemId) throws ItemNotFoundException {

        AllocationSummaryResponse summary = allocationService.getAllocationSummary(
                organisationId, budgetId, headerLineItemId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Header allocation summary retrieved successfully", summary));
    }

    @GetMapping("/all")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllBudgetAllocations(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        List<OrgBudgetDetailAllocationEntity> allocations = allocationService.getAllBudgetAllocations(
                organisationId, budgetId);

        List<DetailAllocationResponse> responses = allocations.stream()
                .map(this::mapToAllocationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "All budget allocations retrieved successfully", responses));
    }

    // Mapping method
    private DetailAllocationResponse mapToAllocationResponse(OrgBudgetDetailAllocationEntity allocation) {
        DetailAllocationResponse response = new DetailAllocationResponse();
        response.setAllocationId(allocation.getAllocationId());
        response.setHeaderLineItemId(allocation.getHeaderLineItem().getLineItemId());
        response.setHeaderAccountCode(allocation.getHeaderAccountCode());
        response.setHeaderAccountName(allocation.getHeaderAccountName());
        response.setDetailAccountId(allocation.getDetailAccount().getId());
        response.setDetailAccountCode(allocation.getDetailAccountCode());
        response.setDetailAccountName(allocation.getDetailAccountName());
        response.setAllocatedAmount(allocation.getAllocatedAmount());
        response.setSpentAmount(allocation.getSpentAmount());
        response.setCommittedAmount(allocation.getCommittedAmount());
        response.setRemainingAmount(allocation.getRemainingAmount());
        response.setAllocationNotes(allocation.getAllocationNotes());
        response.setHasAllocation(allocation.hasAllocation());
        response.setAllocationStatus(allocation.getAllocationStatus());
        response.setUtilizationPercentage(allocation.getUtilizationPercentage());
        response.setCreatedDate(allocation.getCreatedDate());
        response.setModifiedDate(allocation.getModifiedDate());
        return response;
    }
}
