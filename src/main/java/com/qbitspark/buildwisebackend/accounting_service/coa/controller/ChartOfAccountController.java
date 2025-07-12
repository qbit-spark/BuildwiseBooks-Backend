package com.qbitspark.buildwisebackend.accounting_service.coa.controller;

import com.qbitspark.buildwisebackend.accounting_service.coa.payload.AddAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/coa")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService chartOfAccountService;

    @GetMapping
    public ResponseEntity<GroupedChartOfAccountsResponse> getGroupedHierarchicalChartOfAccounts(
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        GroupedChartOfAccountsResponse response = chartOfAccountService
                .getGroupedHierarchicalChartOfAccounts(organisationId);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/initialize")
    public ResponseEntity<GlobeSuccessResponseBuilder> initializeDefaultChartOfAccounts(
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        GroupedChartOfAccountsResponse response = chartOfAccountService
                .initializeDefaultChartOfAccounts(organisationId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Default chart of accounts initialized successfully", response));
    }


    @PostMapping("/add")
    public ResponseEntity<GlobeSuccessResponseBuilder> addNewAccount(
            @Valid @RequestBody AddAccountRequest request,
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        ChartOfAccountsResponse response = chartOfAccountService.addNewAccount(organisationId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Account created successfully", response));
    }


    @PutMapping("/{accountId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateAccount(
            @PathVariable UUID accountId,
            @PathVariable UUID organisationId,
            @Valid @RequestBody AddAccountRequest request) throws ItemNotFoundException, AccessDeniedException {

        ChartOfAccountsResponse response = chartOfAccountService.updateAccount(organisationId, accountId, request);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Account updated successfully", response));
    }

}