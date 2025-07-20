package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.controller;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailDistributionEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.*;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.BudgetFundingService;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/budgets")
@RequiredArgsConstructor
public class OrgBudgetController {

    private final OrgBudgetService orgBudgetService;
    private final BudgetFundingService budgetFundingService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createBudget(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateBudgetRequest request) throws ItemNotFoundException, AccessDeniedException {

        OrgBudgetEntity createdBudget = orgBudgetService.createBudget(request, organisationId);
        CreateBudgetResponse response = mapToResponse(createdBudget);


        return new ResponseEntity<>(
                GlobeSuccessResponseBuilder.success("Budget created successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{budgetId}/detailed-allocation-summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getBudgetAllocationSummary(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException, AccessDeniedException {

        BudgetAllocationResponse response = orgBudgetService
                .getBudgetAllocationSummary(budgetId, organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Budget allocation summary retrieved successfully", response));
    }

    @PostMapping("/{budgetId}/detail-distribution")
    public ResponseEntity<GlobeSuccessResponseBuilder> distributeToDetails(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody DistributeToDetailsRequest request, @RequestParam(value = "action") ActionType action)
            throws ItemNotFoundException, AccessDeniedException {


        List<OrgBudgetDetailDistributionEntity> distributions =
                orgBudgetService.distributeToDetails(budgetId, request, organisationId, action);

        List<DistributionCreatedResponse> responses = distributions.stream()
                .map(this::mapToCreatedResponse)
                .collect(Collectors.toList());

        String successMessage = switch (action) {
            case SAVE -> "Budget distribution saved successfully";
            case SAVE_AND_APPROVAL -> "Budget distribution saved and submitted for approval";
        };


        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                successMessage, responses));
    }

    @GetMapping("/{budgetId}/distribution-details")
    public ResponseEntity<GlobeSuccessResponseBuilder> getBudgetDistributionDetails(
            @PathVariable UUID organisationId,
            @PathVariable(required = false) UUID budgetId) throws ItemNotFoundException, AccessDeniedException {

        BudgetDistributionDetailResponse response = orgBudgetService
                .getBudgetDistributionDetails(budgetId, organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Budget distribution details retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getBudgets(
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        List<OrgBudgetEntity> budgets = orgBudgetService.getBudgets(organisationId);

        List<BudgetListResponse> budgetResponses = budgets.stream()
                .map(this::mapToBudgetListResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Budgets retrieved successfully", budgetResponses));
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody UpdateBudgetRequest request,
            @RequestParam(value = "action") ActionType action) throws ItemNotFoundException, AccessDeniedException {

        OrgBudgetEntity updatedBudget = orgBudgetService.updateBudget(budgetId, request, organisationId, action);
        CreateBudgetResponse response = mapToResponse(updatedBudget);

        String successMessage = switch (action) {
            case SAVE -> "Budget saved successfully";
            case SAVE_AND_APPROVAL -> "Budget saved and submitted for approval";
        };

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                successMessage, response));
    }


    @PutMapping("/{budgetId}/activate")
    public ResponseEntity<GlobeSuccessResponseBuilder> activateBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException, AccessDeniedException {

        orgBudgetService.activateBudget(budgetId, organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Budget activated successfully. Previous active budget has been closed."));
    }

    @GetMapping("/available-to-spend/summary-list")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAvailableDetailAllocations(
            @PathVariable UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        List<AvailableDetailAllocationResponse> allocations = budgetFundingService.getAvailableDetailAllocations(organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Available detail allocations retrieved successfully", allocations));
    }

    // Simple mapping methods
    private CreateBudgetResponse mapToResponse(OrgBudgetEntity budget) {
        CreateBudgetResponse response = new CreateBudgetResponse();
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setStatus(budget.getStatus());
        response.setDescription(budget.getDescription());
        response.setCreatedDate(budget.getCreatedDate());
        return response;
    }

    private BudgetListResponse mapToBudgetListResponse(OrgBudgetEntity budget) {
        BudgetListResponse response = new BudgetListResponse();
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setStatus(budget.getStatus());
        response.setCreatedDate(budget.getCreatedDate());
        return response;
    }


    private DistributionCreatedResponse mapToCreatedResponse(OrgBudgetDetailDistributionEntity distribution) {
        DistributionCreatedResponse response = new DistributionCreatedResponse();
        response.setDistributionId(distribution.getDistributionId());
        response.setDetailAccountCode(distribution.getDetailAccount().getAccountCode());
        response.setDetailAccountName(distribution.getDetailAccount().getName());
        response.setDistributedAmount(distribution.getDistributedAmount());
        response.setDescription(distribution.getDescription());
        return response;
    }
}