package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class VoucherBeneficiaryRequest {

    @NotNull(message = "Vendor ID is required")
    private UUID vendorId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Valid
    private List<UUID> deductions = new ArrayList<>();

}
