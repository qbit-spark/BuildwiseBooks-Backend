package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.TaxType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class InvoiceLineItemResponse {
    private UUID id;
    private String description;
    private BigDecimal rate;
    private BigDecimal quantity;
    private BigDecimal lineTotal;
    private String unitOfMeasure;
}