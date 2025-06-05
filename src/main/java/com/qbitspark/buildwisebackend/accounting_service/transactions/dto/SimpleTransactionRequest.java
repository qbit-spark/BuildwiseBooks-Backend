package com.qbitspark.buildwisebackend.accounting_service.transactions.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SimpleTransactionRequest {
    private UUID organisationId;
    private UUID projectId; // Optional
    private UUID debitAccountId;
    private UUID creditAccountId;
    private BigDecimal amount;
    private String description;
    private String referenceNumber;
}
