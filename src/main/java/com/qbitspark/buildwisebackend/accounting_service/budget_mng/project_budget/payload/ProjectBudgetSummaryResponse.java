package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProjectBudgetSummaryResponse {
    private UUID accountId;
    private String accountCode;
    private String accountName;
    private String headingParent;
    private BigDecimal budgetRemaining;
    private BigDecimal availableBalance;
    private String notes;
}
