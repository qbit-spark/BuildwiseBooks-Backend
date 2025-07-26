package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateInvoiceDocRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Date of issue is required")
    private LocalDate dateOfIssue;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private BigDecimal creditApplied;

    private String reference;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<InvoiceLineItemRequest> lineItems;

    private List<UUID> taxesToApply;

    private List<UUID> attachments;
}
