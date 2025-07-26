package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
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
public class OrgBudgetDetailedResponse {

    private UUID budgetId;
    private String budgetName;
    private LocalDate financialYearStart;
    private LocalDate financialYearEnd;
    private BigDecimal totalBudgetAmount;
    private BigDecimal distributedAmount;
    private BigDecimal availableAmount;
    private BigDecimal totalSpentAmount;
    private BigDecimal totalCommittedAmount;
    private BigDecimal totalRemainingAmount;
    private OrgBudgetStatus status;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    private UUID organisationId;
    private String organisationName;

    private BigDecimal budgetUtilizationPercentage;
    private BigDecimal spendingPercentage;
    private int totalAccounts;
    private int accountsWithBudget;
    private int accountsWithoutBudget;

    private List<AccountGroupResponse> accountGroups;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountGroupResponse {
        private String headerAccountCode;
        private String headerAccountName;
        private BigDecimal groupTotalBudget;
        private BigDecimal groupTotalSpent;
        private BigDecimal groupTotalCommitted;
        private BigDecimal groupTotalRemaining;
        private int totalAccounts;
        private int accountsWithBudget;
       // private List<OrgBudgetLineItemResponse> lineItems;
    }
}