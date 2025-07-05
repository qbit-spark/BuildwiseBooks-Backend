package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgBudgetLineItemResponse {

    private UUID lineItemId;
    private UUID accountId;
    private String accountCode;
    private String accountName;
    private String accountDescription;
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private BigDecimal committedAmount;
    private BigDecimal remainingAmount;
    private String lineItemNotes;
    private boolean hasBudgetAllocated;
    private BigDecimal utilizationPercentage;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private BigDecimal allocatedToDetails;
    private BigDecimal availableForAllocation;

}