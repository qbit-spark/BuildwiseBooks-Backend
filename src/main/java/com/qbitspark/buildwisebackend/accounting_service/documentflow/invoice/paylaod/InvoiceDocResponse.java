package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceDocResponse {
    private UUID id;
    private String invoiceNumber;
    private UUID projectId;
    private String projectName;
    private UUID clientId;
    private String clientName;
    private InvoiceType invoiceType;
    private InvoiceStatus invoiceStatus;
    private LocalDate dateOfIssue;
    private LocalDate dueDate;
    private String reference;
    private UUID organisationId;
    private String organisationName;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal creditApplied;
    private BigDecimal amountDue;
    private String currency;
    private List<InvoiceLineItemResponse> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdByUserName;
}
