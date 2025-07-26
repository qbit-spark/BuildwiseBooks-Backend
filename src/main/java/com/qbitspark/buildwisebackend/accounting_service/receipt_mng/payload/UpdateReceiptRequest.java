package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateReceiptRequest {

    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private LocalDate receiptDate;
    private UUID bankAccountId;
    private String reference;
    private String description;
    private List<UUID> attachments;
}