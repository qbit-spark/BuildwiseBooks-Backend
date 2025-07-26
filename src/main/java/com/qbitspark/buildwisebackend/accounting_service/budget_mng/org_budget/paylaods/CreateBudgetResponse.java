package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateBudgetResponse {

    private UUID budgetId;
    private String budgetName;
    private LocalDate financialYearStart;
    private LocalDate financialYearEnd;
    private BigDecimal totalBudgetAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal availableAmount;
    private OrgBudgetStatus status;
    private String description;
    private LocalDateTime createdDate;

}