package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VoucherSummaryResponse {
    private UUID voucherId;
    private String voucherNumber;
    private VoucherStatus status;
    private BigDecimal totalAmount;
    private String projectName;
    private Integer payeeCount;
    private LocalDateTime voucherDate;
    private String preparedBy;
}