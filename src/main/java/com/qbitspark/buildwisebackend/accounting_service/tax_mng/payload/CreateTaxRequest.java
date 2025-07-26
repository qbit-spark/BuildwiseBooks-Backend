package com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload;

import lombok.Data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.beans.BeanInfo;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaxRequest {

    @NotBlank(message = "Tax name is required")
    private String taxName;

    @NotNull(message = "Tax percentage is required")
    @DecimalMin(value = "0.0", message = "Tax percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Tax percentage cannot exceed 100%")
    private BigDecimal taxPercent;

    private String taxDescription;

    private Boolean isActive = true; // Default to active
}