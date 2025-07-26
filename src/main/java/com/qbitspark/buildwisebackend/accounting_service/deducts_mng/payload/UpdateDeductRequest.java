package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeductRequest {

    private String deductName;

    @DecimalMin(value = "0.0", message = "Deduct percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Deduct percentage cannot exceed 100%")
    private BigDecimal deductPercent;

    private String deductDescription;


    private Boolean isActive;
}
