package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class VoucherBeneficiaryResponse {
    private UUID vendorId;
    private String vendorName;
    private String description;
    private BigDecimal amount;
    private BigDecimal netAmount;
    private List<VoucherDeductionResponse> deductions;
}
