package com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Deprecated
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
