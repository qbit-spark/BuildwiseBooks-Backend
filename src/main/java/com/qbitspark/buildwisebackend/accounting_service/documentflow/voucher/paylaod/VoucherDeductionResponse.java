package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VoucherDeductionResponse {
    private UUID deductionId;
    private BigDecimal percentage;
    private BigDecimal deductionAmount;
}
