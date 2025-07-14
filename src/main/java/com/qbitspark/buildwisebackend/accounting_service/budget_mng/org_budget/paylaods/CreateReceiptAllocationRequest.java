package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class CreateReceiptAllocationRequest {

    @NotNull(message = "Receipt ID is required")
    private UUID receiptId;

    private String notes;

    @NotEmpty(message = "Allocation details cannot be empty")
    @Size(min = 1, max = 50, message = "Must have 1-50 allocation details")
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
        @DecimalMax(value = "999999.99", message = "Amount too large")
        private BigDecimal amount;

        private String description;
    }
}