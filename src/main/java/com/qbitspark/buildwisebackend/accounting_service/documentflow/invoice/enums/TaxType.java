package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum TaxType {
    NO_TAX("No Tax", BigDecimal.ZERO),
    VAT_18("VAT 18%", new BigDecimal("18.00")),
    CUSTOM("Custom", BigDecimal.ZERO);

    private final String displayName;
    private final BigDecimal defaultRate;

    TaxType(String displayName, BigDecimal defaultRate) {
        this.displayName = displayName;
        this.defaultRate = defaultRate;
    }
}