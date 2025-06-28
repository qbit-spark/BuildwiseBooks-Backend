package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.DeductionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherDeductionRequest {

    @NotNull(message = "Deduction type is required")
    private DeductionType deductionType;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.01", message = "Percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Percentage cannot exceed 100")
    private BigDecimal percentage;

}