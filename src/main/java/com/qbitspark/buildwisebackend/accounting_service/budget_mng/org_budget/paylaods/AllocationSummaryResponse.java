package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AllocationSummaryResponse {

    private UUID headerLineItemId;
    private String headerAccountCode;
    private String headerAccountName;
    private BigDecimal headerBudgetAmount;

    // Allocation totals
    private BigDecimal totalAllocatedAmount;
    private BigDecimal totalSpentAmount;
    private BigDecimal totalCommittedAmount;
    private BigDecimal availableForAllocation;
    private BigDecimal totalRemainingAmount;

    // Account statistics
    private int totalDetailAccounts;
    private int accountsWithAllocation;
    private int accountsWithoutAllocation;

    // Calculated fields
    public BigDecimal getAllocationUtilizationPercentage() {
        if (headerBudgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalAllocatedAmount
                .multiply(BigDecimal.valueOf(100)).divide(headerBudgetAmount, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getSpendingPercentage() {
        if (totalAllocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalSpentAmount.add(totalCommittedAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(totalAllocatedAmount, 2, RoundingMode.HALF_UP);
    }
}
