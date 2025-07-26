package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import jakarta.validation.Valid;
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
public class UpdateReceiptAllocationRequest {

    private String notes;

    @NotEmpty(message = "Allocation details cannot be empty")
    @Valid
    private List<AllocationDetailRequest> allocationDetails;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AllocationDetailRequest {

        @NotNull(message = "Account ID is required")
        private UUID accountId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        private String description;
    }
}