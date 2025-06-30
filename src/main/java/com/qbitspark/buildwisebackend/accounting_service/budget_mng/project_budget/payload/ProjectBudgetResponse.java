package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.enums.ProjectBudgetStatus;
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
public class ProjectBudgetResponse {

    private UUID projectBudgetId;
    private UUID projectId;
    private String projectName;
    private UUID orgBudgetId;
    private String orgBudgetName;

    private BigDecimal totalBudgetAmount;
    private BigDecimal totalSpentAmount;
    private BigDecimal totalCommittedAmount;
    private BigDecimal totalRemainingAmount;

    private ProjectBudgetStatus status;
    private String budgetNotes;
    private LocalDateTime createdDate;

    // Project budget statistics
    private ProjectBudgetStatistics projectStatistics;

    private List<AccountGroupResponse> accountGroups; // Grouped by header accounts

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectBudgetStatistics {

        // Budget Distribution Overview
        private BigDecimal totalBudgetDistributed;
        private BigDecimal totalBudgetAvailable; // From org budget
        private BigDecimal budgetUtilizationPercentage; // How much of org budget is used

        // Account-level Statistics
        private int totalExpenseAccounts;
        private int accountsWithBudgetDistributed;
        private int accountsWithoutBudget;
        private BigDecimal percentageAccountsWithBudget;

        // Spending Statistics
        private BigDecimal totalSpentAmount;
        private BigDecimal totalCommittedAmount;
        private BigDecimal totalAvailableToSpend;
        private BigDecimal spendingPercentage; // % of budget already spent
        private BigDecimal commitmentPercentage; // % of budget committed

        // Group-level Statistics
        private int totalAccountGroups;
        private int groupsWithBudget;
        private int groupsWithoutBudget;

        // Financial Health Indicators
        private String budgetStatus; // "Under Budget", "On Track", "Over Budget", "At Risk"
        private boolean hasOverspendingRisk;
        private BigDecimal projectedBudgetShortfall; // If spending trends continue
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountGroupResponse {

        private UUID headerAccountId;
        private String headerAccountCode;
        private String headerAccountName;
        private String headerAccountDescription;

        // Group statistics
        private BigDecimal groupTotalBudget;
        private BigDecimal groupTotalSpent;
        private BigDecimal groupTotalCommitted;
        private BigDecimal groupTotalRemaining;

        private int totalAccounts;
        private int accountsWithBudget;
        private int accountsWithoutBudget;

        private List<LineItemResponse> lineItems; // Detail accounts under this header
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LineItemResponse {

        private UUID budgetLineItemId; // null if no budget distributed
        private UUID coaAccountId;
        private String accountCode;
        private String accountName;

        private BigDecimal budgetAmount;
        private BigDecimal spentAmount;
        private BigDecimal committedAmount;
        private BigDecimal remainingAmount;

        private String lineItemNotes;
        private boolean hasBudgetDistributed; // true if budget distributed, false if $0
    }
}