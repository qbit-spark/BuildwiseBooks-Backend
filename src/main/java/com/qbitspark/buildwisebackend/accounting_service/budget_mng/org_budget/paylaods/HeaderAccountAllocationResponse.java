package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HeaderAccountAllocationResponse {
    private UUID headerLineItemId;
    private UUID headerAccountId;
    private String headerAccountCode;
    private String headerAccountName;
    private String headerDescription;
    private BigDecimal headerBudgetAmount;
    private BigDecimal headerAllocatedToDetails;
    private BigDecimal headerAvailableForAllocation;
    private BigDecimal headerSpentAmount;
    private BigDecimal headerCommittedAmount;
    private BigDecimal headerRemainingAmount;
    private BigDecimal headerUtilizationPercentage;
    private BigDecimal headerAllocationPercentage;
    private boolean hasBudgetAllocated;
    private int detailAccountCount;
    private int detailsWithAllocation;
    private int detailsWithoutAllocation;
    private List<DetailAccountAllocationResponse> detailAccounts;
}
