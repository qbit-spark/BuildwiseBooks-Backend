package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import jakarta.validation.constraints.NotBlank;
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
public class CreateBudgetRequest {

    @NotNull(message = "Financial year start date is required")
    private LocalDate financialYearStart;

    @NotNull(message = "Financial year end date is required")
    private LocalDate financialYearEnd;

    private String description;

}
