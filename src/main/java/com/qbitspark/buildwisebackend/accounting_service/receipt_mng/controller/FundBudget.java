package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.controller;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.ReceiptAllocationService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


//Todo: NOte
/***
 *
 *
 * This is for testing only won't go in production
 *
 *
 */
@RestController
@RequestMapping("api/v1/fund-budget-from-approved-allocation/{organisationId}")
@RequiredArgsConstructor
public class FundBudget {

    private final ReceiptAllocationService receiptAllocationService;

    @GetMapping("/{allocationId}")
    public String getFundBudgetFromApprovedAllocation(@PathVariable UUID organisationId, @PathVariable UUID allocationId) throws AccessDeniedException, ItemNotFoundException {
        receiptAllocationService.fundBudget(organisationId, allocationId);

        return "Budget successfully funded";
    }
}
