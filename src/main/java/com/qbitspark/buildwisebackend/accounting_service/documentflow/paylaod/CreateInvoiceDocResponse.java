package com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateInvoiceDocResponse {
    private UUID invoiceId;
    private String invoiceNumber;
    private String status;
    private BigDecimal totalAmount;
    private String projectName;
    private String clientName;
}
