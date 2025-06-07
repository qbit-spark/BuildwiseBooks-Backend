package com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TransactionResponse {
    private UUID journalEntryId;
    private String description;
    private String referenceNumber;
    private LocalDateTime transactionDateTime;
    private UUID organisationId;
    private UUID projectId;
    private String projectName;
    private String transactionLevel;
    private BigDecimal totalAmount;
    private int lineCount;
}
