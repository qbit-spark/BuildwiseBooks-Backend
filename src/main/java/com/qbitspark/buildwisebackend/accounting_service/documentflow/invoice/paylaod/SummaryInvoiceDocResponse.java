package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SummaryInvoiceDocResponse {
    private UUID invoiceId;
    private String invoiceNumber;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private String projectName;
    private String clientName;
    private Integer lineItemCount;
}
