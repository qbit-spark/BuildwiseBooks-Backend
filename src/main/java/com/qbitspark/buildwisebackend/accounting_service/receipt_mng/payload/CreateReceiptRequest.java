package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateReceiptRequest {

    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Receipt date is required")
    private LocalDate receiptDate;

    private UUID bankAccountId;
    private String reference;
    private String description;
    private List<UUID> attachments;
}