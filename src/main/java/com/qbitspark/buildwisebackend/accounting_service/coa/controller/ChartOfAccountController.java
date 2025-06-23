package com.qbitspark.buildwisebackend.accounting_service.coa.controller;

import com.qbitspark.buildwisebackend.accounting_service.coa.payload.AddAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/chart-of-accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService chartOfAccountService;

    /**
     * Get a chart of accounts for organization
     */
    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GroupedChartOfAccountsResponse> getGroupedHierarchicalChartOfAccounts(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        GroupedChartOfAccountsResponse response = chartOfAccountService
                .getGroupedHierarchicalChartOfAccounts(organisationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Initialize default chart of accounts for organization (one-time setup)
     */
    @PostMapping("/initialize/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> initializeDefaultChartOfAccounts(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        GroupedChartOfAccountsResponse response = chartOfAccountService
                .initializeDefaultChartOfAccounts(organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Default chart of accounts initialized successfully", response));
    }

    /**
     * Add a new account to existing chart of accounts
     */
    @PostMapping("/add")
    public ResponseEntity<GlobeSuccessResponseBuilder> addNewAccount(
            @Valid @RequestBody AddAccountRequest request) throws ItemNotFoundException {

        ChartOfAccountsResponse response = chartOfAccountService.addNewAccount(request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Account created successfully", response));
    }

    /**
     * Update an existing account
     */
    @PutMapping("/{accountId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AddAccountRequest request) throws ItemNotFoundException {

        ChartOfAccountsResponse response = chartOfAccountService.updateAccount(accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Account updated successfully", response));
    }
}