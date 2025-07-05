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

    // Allocate money to detail accounts within a header account
    @PutMapping("/header/{headerLineItemId}/allocate")
    public ResponseEntity<GlobeSuccessResponseBuilder> allocateMoneyToDetailAccounts(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @PathVariable UUID headerLineItemId,
            @Valid @RequestBody AllocateMoneyRequest request) throws ItemNotFoundException {

        List<OrgBudgetDetailAllocationEntity> allocations = allocationService.allocateMoneyToDetailAccounts(
                organisationId, budgetId, headerLineItemId, request);

        List<DetailAllocationResponse> responses = allocations.stream()
                .map(this::mapToAllocationResponse)
                .collect(Collectors.toList());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Money allocated to detail accounts successfully",
                responses
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    // Initialize detail account allocations for a header (with $0)
    @PostMapping("/header/{headerLineItemId}/initialize")
    public ResponseEntity<GlobeSuccessResponseBuilder> initializeDetailAllocations(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @PathVariable UUID headerLineItemId) throws ItemNotFoundException {

        allocationService.initializeDetailAllocations(headerLineItemId, organisationId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Detail account allocations initialized successfully"
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    // Get all allocations for a header account
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

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Header allocations retrieved successfully",
                responses
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    // Get allocation summary for a header account
    @GetMapping("/header/{headerLineItemId}/summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getHeaderAllocationSummary(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @PathVariable UUID headerLineItemId) throws ItemNotFoundException {

        AllocationSummaryResponse summary = allocationService.getAllocationSummary(
                organisationId, budgetId, headerLineItemId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Header allocation summary retrieved successfully",
                summary
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    // Get all allocations for the entire budget
    @GetMapping("/all")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllBudgetAllocations(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        List<OrgBudgetDetailAllocationEntity> allocations = allocationService.getAllBudgetAllocations(
                organisationId, budgetId);

        List<DetailAllocationResponse> responses = allocations.stream()
                .map(this::mapToAllocationResponse)
                .collect(Collectors.toList());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "All budget allocations retrieved successfully",
                responses
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    // Get available detail accounts for allocation under a header
    @GetMapping("/header/{headerLineItemId}/available-accounts")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAvailableDetailAccounts(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @PathVariable UUID headerLineItemId) throws ItemNotFoundException {

        // Get header allocations to check existing allocations
        List<OrgBudgetDetailAllocationEntity> existingAllocations = allocationService.getHeaderAllocations(
                organisationId, budgetId, headerLineItemId);

        // Get available detail accounts
        List<AvailableDetailAccountResponse> availableAccounts = getDetailAccountsForHeader(
                organisationId, headerLineItemId, existingAllocations);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Available detail accounts retrieved successfully",
                availableAccounts
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    // Mapping methods
    private DetailAllocationResponse mapToAllocationResponse(OrgBudgetDetailAllocationEntity allocation) {
        DetailAllocationResponse response = new DetailAllocationResponse();
        response.setAllocationId(allocation.getAllocationId());

        // Header account info
        response.setHeaderLineItemId(allocation.getHeaderLineItem().getLineItemId());
        response.setHeaderAccountCode(allocation.getHeaderAccountCode());
        response.setHeaderAccountName(allocation.getHeaderAccountName());

        // Detail account info
        response.setDetailAccountId(allocation.getDetailAccount().getId());
        response.setDetailAccountCode(allocation.getDetailAccountCode());
        response.setDetailAccountName(allocation.getDetailAccountName());

        // Allocation amounts
        response.setAllocatedAmount(allocation.getAllocatedAmount());
        response.setSpentAmount(allocation.getSpentAmount());
        response.setCommittedAmount(allocation.getCommittedAmount());
        response.setRemainingAmount(allocation.getRemainingAmount());

        // Metadata
        response.setAllocationNotes(allocation.getAllocationNotes());
        response.setHasAllocation(allocation.hasAllocation());
        response.setAllocationStatus(allocation.getAllocationStatus());
        response.setUtilizationPercentage(allocation.getUtilizationPercentage());
        response.setCreatedDate(allocation.getCreatedDate());
        response.setModifiedDate(allocation.getModifiedDate());

        return response;
    }

    private List<AvailableDetailAccountResponse> getDetailAccountsForHeader(
            UUID organisationId, UUID headerLineItemId, List<OrgBudgetDetailAllocationEntity> existingAllocations)
            throws ItemNotFoundException {

        // Get organisation
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Get all expense detail accounts
        List<ChartOfAccounts> detailAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(account -> !account.getIsHeader()) // Only detail accounts
                .filter(ChartOfAccounts::getIsPostable) // Only postable accounts
                .toList();

        // Map existing allocations for a quick lookup
        var allocationMap = existingAllocations.stream()
                .collect(Collectors.toMap(
                        alloc -> alloc.getDetailAccount().getId(),
                        alloc -> alloc
                ));

        // Map to response
        return detailAccounts.stream()
                .map(account -> {
                    AvailableDetailAccountResponse response = new AvailableDetailAccountResponse();
                    response.setDetailAccountId(account.getId());
                    response.setAccountCode(account.getAccountCode());
                    response.setAccountName(account.getName());
                    response.setDescription(account.getDescription());
                    response.setPostable(account.getIsPostable());
                    response.setActive(account.getIsActive());

                    // Check if allocation exists
                    OrgBudgetDetailAllocationEntity existingAllocation = allocationMap.get(account.getId());
                    if (existingAllocation != null) {
                        response.setCurrentAllocation(existingAllocation.getAllocatedAmount());
                        response.setRemainingAmount(existingAllocation.getRemainingAmount());
                        response.setHasExistingAllocation(true);
                    } else {
                        response.setCurrentAllocation(BigDecimal.ZERO);
                        response.setRemainingAmount(BigDecimal.ZERO);
                        response.setHasExistingAllocation(false);
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }
}