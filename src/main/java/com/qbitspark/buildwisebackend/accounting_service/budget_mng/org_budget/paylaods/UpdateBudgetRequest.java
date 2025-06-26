package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBudgetRequest {

    private LocalDate financialYearStart;
    private LocalDate financialYearEnd;

    @Positive(message = "Total budget amount must be positive")
    private BigDecimal totalBudgetAmount;

    private String description;

}
