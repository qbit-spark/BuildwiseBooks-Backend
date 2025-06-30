package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherDeductionResponse {
    private BigDecimal percentage;
    private BigDecimal deductionAmount;
}
