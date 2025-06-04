package com.qbitspark.buildwisebackend.accounting_service.controller;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/chart-of-accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService chartOfAccountService;

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getChartOfAccountsByOrganisation(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<ChartOfAccountsResponse> accounts = chartOfAccountService.getChartOfAccountsByOrganisationId(organisationId); // CHANGE TYPE

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Chart of accounts retrieved successfully",
                accounts
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/organisation/{organisationId}/grouped-hierarchical")
    public ResponseEntity<GroupedChartOfAccountsResponse> getGroupedHierarchicalChartOfAccounts(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

            GroupedChartOfAccountsResponse response = chartOfAccountService
                    .getGroupedHierarchicalChartOfAccounts(organisationId);
            return ResponseEntity.ok(response);
    }
}
