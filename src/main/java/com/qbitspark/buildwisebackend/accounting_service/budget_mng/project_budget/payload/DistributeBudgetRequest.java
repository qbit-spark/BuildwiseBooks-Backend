package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DistributeBudgetRequest {

    @NotEmpty(message = "Account distributions cannot be empty")
    private List<AccountDistribution> accountDistributions;

    private String notes;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountDistribution {

        @NotNull(message = "Account ID is required")
        private UUID accountId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        private String description;
    }

}