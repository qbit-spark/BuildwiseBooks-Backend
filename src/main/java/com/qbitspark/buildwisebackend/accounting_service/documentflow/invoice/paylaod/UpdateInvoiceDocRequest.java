package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import jakarta.validation.Valid;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateInvoiceDocRequest {

    private LocalDate dateOfIssue;
    private LocalDate dueDate;
    private BigDecimal creditApplied;
    private String reference;

    @Valid
    private List<InvoiceLineItemRequest> lineItems;

    private List<UUID> taxesToApply;
}