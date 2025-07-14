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
public class CreateReceiptAllocationRequest {

    @NotNull(message = "Receipt ID is required")
    private UUID receiptId;

    @NotEmpty(message = "At least one detail allocation is required")
    @Valid
    private List<DetailAllocationRequest> detailAllocations;

    private String notes;

    @Data
    public static class DetailAllocationRequest {
        @NotNull(message = "Detail account ID is required")
        private UUID detailAccountId;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private BigDecimal amount;

        private String description;
    }
}