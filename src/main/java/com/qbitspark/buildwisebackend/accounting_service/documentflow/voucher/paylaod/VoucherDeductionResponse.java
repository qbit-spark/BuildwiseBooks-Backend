package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.DeductionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoucherDeductionResponse {
    private DeductionType deductionType;
    private BigDecimal percentage;
    private BigDecimal deductionAmount;
}
