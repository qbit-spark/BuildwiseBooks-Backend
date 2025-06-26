package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.controller;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.BudgetListResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateBudgetResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.UpdateBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetService;
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

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createBudget(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateBudgetRequest request) throws ItemNotFoundException {

        OrgBudgetEntity createdBudget = orgBudgetService.createBudget(request, organisationId);
        CreateBudgetResponse response = mapToResponse(createdBudget);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget created successfully",
                response
        );

        return new ResponseEntity<>(successResponse, HttpStatus.CREATED);
    }

    @PutMapping("/{budgetId}/activate")
    public ResponseEntity<GlobeSuccessResponseBuilder> activateBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        orgBudgetService.activateBudget(budgetId, organisationId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget activated successfully. Previous active budget has been closed."
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getBudgets(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<OrgBudgetEntity> budgets = orgBudgetService.getBudgets(organisationId);

        // Controller maps entities to response payloads
        List<BudgetListResponse> budgetResponses = budgets.stream()
                .map(this::mapToBudgetListResponse)
                .collect(Collectors.toList());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budgets retrieved successfully",
                budgetResponses
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody UpdateBudgetRequest request) throws ItemNotFoundException {

        OrgBudgetEntity updatedBudget = orgBudgetService.updateBudget(budgetId, request, organisationId);
        CreateBudgetResponse response = mapToResponse(updatedBudget);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget updated successfully",
                response
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    private CreateBudgetResponse mapToResponse(OrgBudgetEntity budget) {
        CreateBudgetResponse response = new CreateBudgetResponse();
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setTotalBudgetAmount(budget.getTotalBudgetAmount());
        response.setAllocatedAmount(budget.getAllocatedAmount());
        response.setAvailableAmount(budget.getAvailableAmount());
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
        response.setTotalBudgetAmount(budget.getTotalBudgetAmount());
        response.setAllocatedAmount(budget.getAllocatedAmount());
        response.setAvailableAmount(budget.getAvailableAmount());
        response.setStatus(budget.getStatus());
        response.setCreatedDate(budget.getCreatedDate());
        return response;
    }

}