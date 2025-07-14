package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BudgetDistributionDetailResponse {

    // Budget Info
    private UUID budgetId;
    private String budgetName;
    private LocalDate financialYearStart;
    private LocalDate financialYearEnd;
    private String budgetStatus;

    // Distribution Summary
    private BigDecimal totalDistributedAmount;
    private int totalDetailAccounts;
    private int detailsWithDistribution;
    private int detailsWithoutDistribution;

    // Header Accounts with Details
    private List<HeaderAccountDistribution> headerAccounts;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HeaderAccountDistribution {

        // Header Info
        private UUID headerAccountId;
        private String headerAccountCode;
        private String headerAccountName;
        private BigDecimal headerTotalDistributed;

        // Header Stats
        private int detailAccountCount;
        private int detailsWithDistribution;
        private int detailsWithoutDistribution;

        // Detail Accounts
        private List<DetailAccountDistribution> detailAccounts;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DetailAccountDistribution {

        private UUID distributionId;
        private UUID detailAccountId;
        private String detailAccountCode;
        private String detailAccountName;
        private String detailAccountDescription;

        // Distribution Info
        private BigDecimal distributedAmount;
        private String description;
        private boolean hasDistribution;
        private String distributionStatus;
        private LocalDateTime createdDate;
    }
}