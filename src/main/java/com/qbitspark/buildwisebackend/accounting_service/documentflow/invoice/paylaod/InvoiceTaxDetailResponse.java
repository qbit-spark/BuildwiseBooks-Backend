package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class InvoiceTaxDetailResponse {
    private UUID originalTaxId;
    private String taxName;
    private BigDecimal taxPercent;
    private String taxDescription;
    private BigDecimal taxableAmount;
    private BigDecimal taxAmount;
}
