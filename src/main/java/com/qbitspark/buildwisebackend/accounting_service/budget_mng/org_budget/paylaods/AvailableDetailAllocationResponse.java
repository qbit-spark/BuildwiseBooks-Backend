package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvailableDetailAllocationResponse {

    private UUID allocationId;
    private String accountCode;
    private String accountName;
    private String headingParent;
    private BigDecimal budgetRemaining;
    private BigDecimal availableBalance;
    private String notes;

    private UUID detailAccountId;
    private UUID headerLineItemId;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal committedAmount;
    private String allocationStatus;
    private boolean hasAllocation;
}