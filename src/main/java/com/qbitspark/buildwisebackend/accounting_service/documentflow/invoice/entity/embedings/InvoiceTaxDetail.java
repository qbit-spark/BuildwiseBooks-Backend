package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.embedings;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceTaxDetail {
    private UUID originalTaxId;
    private String taxName;
    private BigDecimal taxPercent;
    private String taxDescription;
    private BigDecimal taxableAmount;
    private BigDecimal taxAmount;
}