package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateAllocationRequest {

    @NotNull(message = "Receipt ID is required")
    private UUID receiptId;

    @NotEmpty(message = "At least one allocation detail is required")
    @Valid
    private List<AllocationDetail> details;

    private String notes;

    private List<UUID> attachments;

    @Data
    public static class AllocationDetail {
        @NotNull(message = "Budget detail allocation ID is required")
        private UUID budgetDetailAllocationId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

    }
}