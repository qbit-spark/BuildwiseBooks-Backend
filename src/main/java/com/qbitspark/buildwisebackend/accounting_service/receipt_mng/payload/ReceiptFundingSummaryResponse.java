package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public  class ReceiptFundingSummaryResponse {
    private UUID receiptId;
    private String receiptNumber;
    private BigDecimal totalAmount;
    private BigDecimal allocatedAmount;
    private BigDecimal remainingAmount;
    private String projectName;
    private String clientName;
}
