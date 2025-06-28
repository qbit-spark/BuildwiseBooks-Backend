package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VoucherSummaryResponse {
    private UUID id;
    private String voucherNumber;
    private String generalDescription;
    private VoucherStatus status;
    private BigDecimal netAmount;
    private int numberOfBeneficiaries;
}