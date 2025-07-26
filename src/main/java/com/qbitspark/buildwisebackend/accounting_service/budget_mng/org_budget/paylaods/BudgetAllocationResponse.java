package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BudgetAllocationResponse {
    private UUID budgetId;
    private String budgetName;
    private String budgetStatus;
    private String financialYearStart;
    private String financialYearEnd;
    private BigDecimal totalBudgetAmount;
    private BigDecimal totalAllocatedToDetails;
    private BigDecimal totalSpentAmount;
    private BigDecimal totalCommittedAmount;
    private BigDecimal totalRemainingAmount;
    private BigDecimal availableForAllocation;
    private BigDecimal budgetUtilizationPercentage;
    private BigDecimal spendingPercentage;
    private int totalHeaderAccounts;
    private int headersWithBudget;
    private int headersWithoutBudget;
    private int totalDetailAccounts;
    private int detailsWithAllocation;
    private int detailsWithoutAllocation;
    private LocalDateTime createdAt;
    private List<HeaderAccountAllocationResponse> headerAccounts;
}