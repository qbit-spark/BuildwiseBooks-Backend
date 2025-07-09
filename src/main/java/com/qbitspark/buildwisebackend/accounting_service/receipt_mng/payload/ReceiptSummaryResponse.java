package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.PaymentMethod;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ReceiptSummaryResponse {
    private UUID receiptId;
    private String receiptNumber;
    private LocalDate receiptDate;
    private String clientName;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private ReceiptStatus status;
    private String projectName;
    private String reference;
}
