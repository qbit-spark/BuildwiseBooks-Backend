package com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.enums.TaxType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceLineItemRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", message = "Rate must be positive")
    private BigDecimal rate;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", message = "Quantity must be positive")
    private BigDecimal quantity;

    private TaxType taxType = TaxType.NO_TAX;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private String unitOfMeasure;
    private Integer lineOrder = 0;

}