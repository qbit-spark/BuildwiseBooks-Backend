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

        List