package com.qbitspark.buildwisebackend.accounting_service.transactions.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ExpenseTransactionRequest {
    private UUID organisationId;
    private UUID projectId; // Optional
    private UUID vendorId;
    private UUID expenseAccountId;
    private BigDecimal amount;
    private String category;
    private boolean isPaid;
    private String description;
    private String referenceNumber;
}
