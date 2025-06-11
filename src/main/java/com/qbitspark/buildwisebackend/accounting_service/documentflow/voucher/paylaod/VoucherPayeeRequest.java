package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VoucherPayeeRequest {

    @NotNull(message = "Vendor ID is required")
    private UUID vendorId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;
}