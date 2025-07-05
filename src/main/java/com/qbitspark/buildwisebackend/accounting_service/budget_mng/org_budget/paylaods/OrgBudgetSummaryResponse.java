package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgBudgetSummaryResponse {

    private UUID budgetId;
    private String budgetName;
    private BigDecimal totalBudgetAmount;
    private BigDecimal distributedAmount;
    private BigDecimal availableAmount;
    private BigDecimal totalSpentAmount;
    private BigDecimal totalCommittedAmount;
    private BigDecimal totalRemainingAmount;
    private OrgBudgetStatus status;
    private LocalDate financialYearStart;
    private LocalDate financialYearEnd;
    private BigDecimal budgetUtilizationPercentage;
    private BigDecimal spendingPercentage;


    private int totalAccounts;
    private int accountsWithBudget;
    private int accountsWithoutBudget;


    private List<AccountGroupSummary> accountGroups;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountGroupSummary {
        private String headerAccountName;
        private String headerAccountCode;
        private BigDecimal groupBudgetAmount;
        private BigDecimal groupSpentAmount;
        private BigDecimal groupRemainingAmount;
        private int accountCount;
        private int accountsWithBudget;
    }
}
