package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.PaymentMethod;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PaymentHistoryResponse {

    private UUID receiptId;
    private String receiptNumber;
    private LocalDate receiptDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String reference;
    private BigDecimal cumulativeTotal;
    private ReceiptStatus status;
}