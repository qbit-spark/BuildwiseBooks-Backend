package com.qbitspark.buildwisebackend.accounting_service.transactions.dto;

import com.qbitspark.buildwisebackend.accounting_service.transactions.events.user_transaction.InvoiceEvent;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceTransactionRequest {
    private UUID organisationId;
    private UUID projectId; // Optional
    private UUID customerId;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String description;
    private String referenceNumber;
    private List<InvoiceEvent.InvoiceLineItem> lineItems;
}
