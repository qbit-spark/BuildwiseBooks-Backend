package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.PaymentMethod;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ReceiptResponse {

    private UUID receiptId;
    private String receiptNumber;
    private LocalDate receiptDate;
    private UUID organisationId;
    private UUID projectId;
    private UUID clientId;
    private UUID invoiceId;
    private UUID bankAccountId;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private ReceiptStatus status;
    private String reference;
    private String description;
    private List<UUID> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String clientName;
    private String invoiceNumber;
    private String projectName;
    private String bankAccountName;
    private BigDecimal invoiceTotal;
    private BigDecimal invoicePreviousPaid;
    private BigDecimal invoiceNewBalance;
    private String invoiceStatus;
}