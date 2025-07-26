package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.embedings.InvoiceTaxDetail;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.List;


@Data
@AllArgsConstructor
public   class TaxCalculationResult {
    private List<InvoiceTaxDetail> taxDetails;
    private MonetaryAmount totalTaxMoney;

    public BigDecimal getTotalTaxAmount() {
        return totalTaxMoney.getNumber().numberValue(BigDecimal.class);
    }
}

