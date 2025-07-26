package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetHierarchyWithAllocationsResponse {

    // Budget Summary
    private UUID budgetId;
    private String budgetName;
    private LocalDate financialYearStart;
    private LocalDate financialYearEnd;
    private OrgBudgetStatus budgetStatus;
    private LocalDateTime createdAt;

    // Budget Totals
    private BigDecimal totalBudgetAmount;
    private BigDecimal totalAllocatedToDetails;
    private BigDecimal totalSpentAmount;
    private BigDecimal totalCommittedAmount;
    private BigDecimal totalRemainingAmount;
    private BigDecimal availableForAllocation;

    // Summary Statistics
    private BigDecimal budgetUtilizationPercentage;
    private BigDecimal spendingPercentage;
    private int totalHeaderAccounts;
    private int headersWithBudget;
    private int headersWithoutBudget;
    private int totalDetailAccounts;
    private int detailsWithAllocation;
    private int detailsWithoutAllocation;

    // Hierarchy Data
    private List<HeaderAccountWithDetails> headerAccounts;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HeaderAccountWithDetails {

        // Header Account Info
        private UUID headerLineItemId;
        private UUID headerAccountId;
        private String headerAccountCode;
        private String headerAccountName;
        private String headerDescription;

        // Header Budget Info
        private BigDecimal headerBudgetAmount;
        private BigDecimal headerAllocatedToDetails;
        private BigDecimal headerAvailableForAllocation;
        private BigDecimal headerSpentAmount;
        private BigDecimal headerCommittedAmount;
        private BigDecimal headerRemainingAmount;

        // Header Statistics
        private BigDecimal headerUtilizationPercentage;
        private BigDecimal headerAllocationPercentage;
        private boolean hasBudgetAllocated;
        private int detailAccountCount;
        private int detailsWithAllocation;
        private int detailsWithoutAllocation;

        // Detail Accounts under this Header
        private List<DetailAccountAllocation> detailAccounts;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class DetailAccountAllocation {

            // Detail Account Info
            private UUID detailAccountId;
            private String detailAccountCode;
            private String detailAccountName;
            private String detailDescription;

            // Allocation Info
            private UUID allocationId;
            private BigDecimal allocatedAmount;
            private BigDecimal spentAmount;
            private BigDecimal committedAmount;
            private BigDecimal budgetRemaining;

            // Status and Metadata
            private String allocationStatus;
            private boolean hasAllocation;
            private String notes;
            private BigDecimal utilizationPercentage;

            // Timestamps
            private LocalDateTime allocationCreatedDate;
            private LocalDateTime allocationModifiedDate;
        }
    }
}