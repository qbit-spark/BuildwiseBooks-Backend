package com.qbitspark.buildwisebackend.accounting_service.coa.controller;

import com.qbitspark.buildwisebackend.accounting_service.coa.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/chart-of-accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService chartOfAccountService;

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GroupedChartOfAccountsResponse> getGroupedHierarchicalChartOfAccounts(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

            GroupedChartOfAccountsResponse response = chartOfAccountService
                    .getGroupedHierarchicalChartOfAccounts(organisationId);
            return ResponseEntity.ok(response);
    }
}
